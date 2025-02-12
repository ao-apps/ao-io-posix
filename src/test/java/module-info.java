/*
 * ao-io-posix - Java interface to native POSIX filesystem objects.
 * Copyright (C) 2025  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-io-posix.
 *
 * ao-io-posix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-io-posix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-io-posix.  If not, see <https://www.gnu.org/licenses/>.
 */
module com.aoapps.io.posix.tests {
  exports com.aoapps.hodgepodge.io.tests;
  requires com.aoapps.io.posix;
  // Direct
  requires com.aoapps.lang; // <groupId>com.aoapps</groupId><artifactId>ao-lang</artifactId>
  // Java SE
  requires java.logging;
  // Test Direct
  requires com.aoapps.hodgepodge; // <groupId>com.aoapps</groupId><artifactId>ao-hodgepodge</artifactId>
  requires junit; // <groupId>junit</groupId><artifactId>junit</artifactId>
}
