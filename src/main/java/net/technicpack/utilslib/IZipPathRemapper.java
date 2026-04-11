package net.technicpack.utilslib;

/**
 * Remaps zip entry names during extraction. Return the remapped path, or null to skip the entry.
 */
public interface IZipPathRemapper {
  /**
   * @param entryName the original zip entry name
   * @return the remapped path relative to the output directory, or null to skip this entry
   */
  String remap(String entryName);
}
