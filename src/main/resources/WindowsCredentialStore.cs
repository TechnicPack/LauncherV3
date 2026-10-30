using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.IO;
using System.Runtime.InteropServices;
using System.Security.AccessControl;
using System.Security.Principal;
using Microsoft.Win32.SafeHandles;

// Embedded in an encoded command; no elevated script is loaded from a user-writable file.
public static class TechnicCredentialStore {
    const uint ReadControl = 0x20000, WriteDac = 0x40000, WriteOwner = 0x80000;
    const uint DirectoryFlag = 0x10, ReparseFlag = 0x400;
    [StructLayout(LayoutKind.Sequential)] struct Info {
        public uint Attributes, CreationLow, CreationHigh, AccessLow, AccessHigh;
        public uint WriteLow, WriteHigh, Volume, SizeHigh, SizeLow, Links, IndexHigh, IndexLow;
    }
    [StructLayout(LayoutKind.Sequential)] struct Privilege {
        public uint Count, Low;
        public int High;
        public uint Attributes;
    }
    [DllImport("kernel32.dll", CharSet=CharSet.Unicode, SetLastError=true)]
    static extern SafeFileHandle CreateFile(string name, uint access, uint share, IntPtr sa,
        uint disposition, uint flags, IntPtr template);
    [DllImport("kernel32.dll", SetLastError=true)]
    static extern bool GetFileInformationByHandle(SafeFileHandle file, out Info info);
    [DllImport("kernel32.dll", CharSet=CharSet.Unicode)]
    static extern uint GetDriveType(string root);
    [DllImport("kernel32.dll")] static extern IntPtr GetCurrentProcess();
    [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr handle);
    [DllImport("kernel32.dll")] static extern IntPtr LocalFree(IntPtr memory);
    [DllImport("advapi32.dll", SetLastError=true)]
    static extern bool OpenProcessToken(IntPtr process, uint access, out IntPtr token);
    [DllImport("advapi32.dll", CharSet=CharSet.Unicode, SetLastError=true)]
    static extern bool LookupPrivilegeValue(string system, string name, out long luid);
    [DllImport("advapi32.dll", SetLastError=true)]
    static extern bool AdjustTokenPrivileges(IntPtr token, bool disable, ref Privilege state,
        uint size, IntPtr previous, IntPtr required);
    [DllImport("advapi32.dll")]
    static extern uint GetSecurityInfo(SafeFileHandle file, int kind, uint requested,
        out IntPtr owner, out IntPtr group, out IntPtr dacl, out IntPtr sacl, out IntPtr descriptor);
    [DllImport("advapi32.dll", SetLastError=true)]
    static extern bool SetKernelObjectSecurity(SafeFileHandle file, uint information, byte[] descriptor);

    static void Check(bool success) {
        if (!success) throw new Win32Exception(Marshal.GetLastWin32Error());
    }

    static void Enable(string name) {
        IntPtr token;
        Check(OpenProcessToken(GetCurrentProcess(), 0x28, out token));
        try {
            long luid;
            Check(LookupPrivilegeValue(null, name, out luid));
            Privilege privilege = new Privilege { Count=1, Low=(uint)luid,
                High=(int)(luid >> 32), Attributes=2 };
            Check(AdjustTokenPrivileges(token, false, ref privilege, 0, IntPtr.Zero, IntPtr.Zero));
            if (Marshal.GetLastWin32Error() != 0) throw new InvalidOperationException("Privilege unavailable");
        } finally { CloseHandle(token); }
    }

    // No share-delete/write: pin objects and prevent concurrent reparse-point conversion.
    // OPEN_REPARSE_POINT means inspection and ACL writes always address the same object.
    static SafeFileHandle Open(string path, bool directory, uint access, bool missing) {
        SafeFileHandle handle = CreateFile(path, access, 1, IntPtr.Zero, 3, 0x02200000, IntPtr.Zero);
        if (handle.IsInvalid) {
            int error = Marshal.GetLastWin32Error();
            handle.Dispose();
            if (missing && error == 2) return null;
            throw new Win32Exception(error);
        }
        try {
            Info info;
            Check(GetFileInformationByHandle(handle, out info));
            if ((info.Attributes & ReparseFlag) != 0
                || ((info.Attributes & DirectoryFlag) != 0) != directory
                || (!directory && info.Links != 1)) {
                throw new IOException("Unsafe credential store object");
            }
            return handle;
        } catch { handle.Dispose(); throw; }
    }

    static string Owner(SafeFileHandle handle) {
        IntPtr owner, group, dacl, sacl, descriptor;
        uint error = GetSecurityInfo(handle, 1, 1, out owner, out group, out dacl, out sacl, out descriptor);
        if (error != 0) throw new Win32Exception((int)error);
        try { return new SecurityIdentifier(owner).Value; }
        finally { LocalFree(descriptor); }
    }

    static void CheckOwner(SafeFileHandle handle, string sid) {
        string owner = Owner(handle);
        if (owner != sid && owner != "S-1-5-32-544") {
            throw new IOException("Credential store belongs to another Windows user");
        }
    }

    static void SetOwner(SafeFileHandle handle, string sid, bool repair) {
        // No inheritable ACEs: never propagate repairs to arbitrary directory contents.
        // SetKernelObjectSecurity applies to this pinned handle only, unlike path ACL tools.
        string sddl = "O:" + sid + (repair ? "D:P(A;;FA;;;" + sid + ")" : "");
        RawSecurityDescriptor descriptor = new RawSecurityDescriptor(sddl);
        byte[] bytes = new byte[descriptor.BinaryLength];
        descriptor.GetBinaryForm(bytes, 0);
        Check(SetKernelObjectSecurity(handle, repair ? 0x80000005u : 1u, bytes));
    }

    public static void Run(string path, string sid, bool repair) {
        // SID originates from the Java process token, not the elevated account or environment.
        sid = new SecurityIdentifier(sid).Value;
        string root = Path.GetPathRoot(path);
        uint drive = GetDriveType(root);
        if (drive != 2 && drive != 3 && (repair || drive != 4)) {
            throw new IOException("Unsupported credential drive");
        }
        if (repair) {
            Enable("SeBackupPrivilege");
            Enable("SeRestorePrivilege");
        }
        List<SafeFileHandle> handles = new List<SafeFileHandle>();
        try {
            handles.Add(Open(root, true, 0, false));
            string[] parts = path.Substring(root.Length).Split('\\');
            string current = root;
            for (int i = 0; i < parts.Length - 1; i++) {
                current = Path.Combine(current, parts[i]);
                SafeFileHandle ancestor = Open(current, true, 0, !repair);
                if (ancestor == null) {
                    Directory.CreateDirectory(current);
                    ancestor = Open(current, true, 0, false);
                }
                handles.Add(ancestor);
            }
            uint access = ReadControl | WriteOwner | (repair ? WriteDac : 0u);
            SafeFileHandle directory = Open(path, true, access, !repair);
            if (directory == null) {
                Directory.CreateDirectory(path);
                directory = Open(path, true, access, false);
            }
            handles.Add(directory);
            CheckOwner(directory, sid);
            SafeFileHandle credential = Open(Path.Combine(path, "StoredCredential"), false, access, true);
            if (credential != null) {
                handles.Add(credential);
                CheckOwner(credential, sid);
            }
            // Validate both objects before changing either; never enumerate, read or write tokens.
            if (credential != null) SetOwner(credential, sid, repair);
            SetOwner(directory, sid, repair);
        } finally {
            for (int i = handles.Count - 1; i >= 0; i--) handles[i].Dispose();
        }
    }
}
