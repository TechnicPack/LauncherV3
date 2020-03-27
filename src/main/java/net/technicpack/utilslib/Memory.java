/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.utilslib;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class Memory {
    public static final Memory[] memoryOptions = {
            (new Memory(512, "512 MB", 1)),
            (new Memory(768, "768 MB", 2)),
            (new Memory(1024, "1 GB", 0)),
            (new Memory(1536, "1.5 GB", 3)),
            (new Memory(2048, "2 GB", 4)),
            (new Memory(2560, "2.5 GB", 19)),
            (new Memory(3072, "3 GB", 5)),
            (new Memory(3584, "3.5 GB", 20)),
            (new Memory(4096, "4 GB", 6)),
            (new Memory(5120, "5 GB", 7)),
            (new Memory(6144, "6 GB", 8)),
            (new Memory(7168, "7 GB", 9)),
            (new Memory(8192, "8 GB", 10)),
            (new Memory(9216, "9 GB", 11)),
            (new Memory(10240, "10 GB", 12)),
            (new Memory(11264, "11 GB", 13)),
            (new Memory(12288, "12 GB", 14)),
            (new Memory(13312, "13 GB", 15)),
            (new Memory(14336, "14 GB", 16)),
            (new Memory(15360, "15 GB", 17)),
            (new Memory(16384, "16 GB", 18)),
    };
    public static final Memory DEFAULT_MEM = memoryOptions[2];
    public static final int MAX_32_BIT_MEMORY = 1024;

    public static boolean is64Bit() {
        String architecture = System.getProperty("sun.arch.data.model", "32");
        return architecture.equals("64");
    }

    public static long getPhysicalMemory() {
        long maxMemory = 0;
        try {
            OperatingSystemMXBean osInfo = ManagementFactory.getOperatingSystemMXBean();
            if (osInfo instanceof com.sun.management.OperatingSystemMXBean) {
                maxMemory = ((com.sun.management.OperatingSystemMXBean) osInfo).getTotalPhysicalMemorySize() / 1024 / 1024;
            }
        } catch (Throwable t) {
        }
        return Math.max(512, maxMemory);
    }

    public static long getAvailableMemory() {
        return getAvailableMemory(is64Bit());
    }

    public static long getAvailableMemory(boolean is64Bit) {
        long physical = getPhysicalMemory();
        if (!is64Bit && physical > MAX_32_BIT_MEMORY)
            return MAX_32_BIT_MEMORY;
        return physical;
    }

    public static Memory getClosestAvailableMemory(Memory memory) {
        return getClosestAvailableMemory(memory, is64Bit());
    }

    public static Memory getClosestAvailableMemory(Memory memory, boolean is64Bit) {
        long available = getAvailableMemory(is64Bit);
        if (memory.getMemoryMB() <= available)
            return memory;

        Memory bestMemory = Memory.memoryOptions[0];
        for (Memory option : Memory.memoryOptions) {
            if (option.getMemoryMB() <= available && option.getMemoryMB() > bestMemory.getMemoryMB())
                bestMemory = option;
        }

        return bestMemory;
    }

    long memory;
    String text;
    int option;

    private Memory(int memory, String text, int option) {
        this.memory = memory;
        this.text = text;
        this.option = option;
    }

    public long getMemoryMB() {
        return memory;
    }

    public String getDescription() {
        return text;
    }

    public int getSettingsId() {
        return option;
    }

    public static Memory getMemoryFromId(int id) {
        for (Memory m : memoryOptions) {
            if (m.getSettingsId() == id) {
                return m;
            }
        }
        return DEFAULT_MEM;
    }

    public String toString() {
        return getDescription();
    }
}
