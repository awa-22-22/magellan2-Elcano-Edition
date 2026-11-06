// class magellan.test.MagellanTest
// created on Aug 2, 2022
//
// Copyright 2003-2022 by magellan project team
//
// Author : $Author: $
// $Id: $
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program (see doc/LICENCE.txt); if not, write to the
// Free Software Foundation, Inc.,
// 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package magellan.test;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class MagellanTestUtil {

  public static void forceDelete(Path dir) throws IOException {
    if (Files.exists(dir)) {
      Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) throws IOException {
          allow(d);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path f, IOException exc) throws IOException {
          // the walker cannot open a directory without permissions before preVisitDirectory
          if (Files.isDirectory(f, LinkOption.NOFOLLOW_LINKS)) {
            allow(f);
            forceDelete(f);
            return FileVisitResult.CONTINUE;
          }
          throw exc;
        }

        @Override
        public FileVisitResult visitFile(Path f, BasicFileAttributes attrs) throws IOException {
          Files.delete(f);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
          Files.delete(d);
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  public static void del(Path p) {
    try {
      if (Files.exists(p)) {
        Files.delete(p);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Set<PosixFilePermission> permsDeny = Collections.emptySet();
  private static Set<PosixFilePermission> permsAllow =
      EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);

  /**
   * @return true if the file system supports POSIX permissions
   */
  public static boolean isPosix() {
    return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  }

  /**
   * Skips the calling test if permissions cannot be restricted, i.e., on non-POSIX file systems
   * (Windows) or when running as root (who ignores permissions).
   */
  public static void assumePosix() {
    assumeTrue("POSIX permissions not supported", isPosix());
    assumeFalse("running as root", "root".equals(System.getProperty("user.name")));
  }

  /**
   * Restores full owner permissions. Does nothing on non-POSIX file systems.
   */
  public static void allow(Path dir) throws IOException {
    if (isPosix()) {
      Files.setPosixFilePermissions(dir, permsAllow);
    }
  }

  /**
   * Removes all permissions. Does nothing on non-POSIX file systems; tests relying on this should
   * call {@link #assumePosix()} first.
   */
  public static void deny(Path dir) throws IOException {
    if (isPosix()) {
      Files.setPosixFilePermissions(dir, permsDeny);
    }
  }

  public static boolean isInternalTesting() {
    return "INTERNAL".equals(System.getenv("MAGELLAN2_TESTING"));
  }

}
