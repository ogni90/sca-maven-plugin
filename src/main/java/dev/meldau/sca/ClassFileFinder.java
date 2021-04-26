package dev.meldau.sca;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.ListIterator;

/*
 * Copyright 2020-2021 Ingo Meldau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class ClassFileFinder {
  File searchDir;

  public ClassFileFinder(File searchDir) {
    this.searchDir = searchDir;
  }

  public ArrayList<File> getClassFiles() throws FileNotFoundException {
    if (!searchDir.exists()) {
      throw new FileNotFoundException("Directory that should contain class Files not existent.");
    }
    ArrayList<File> classFiles = new ArrayList<>();
    // initialize dirList
    ArrayList<File> dirList = new ArrayList<>();
    // Add output directory to List
    dirList.add(searchDir);
    while (!dirList.isEmpty()) {
      ListIterator<File> dirIterator = dirList.listIterator();
      while (dirIterator.hasNext()) {
        File myDir = dirIterator.next();
        dirIterator.remove();
        // FIXME: does not work for empty Dirs
        for (File myFile : (File[]) ArrayUtils.nullToEmpty(myDir.listFiles())) {
          if (myFile.isDirectory()) {
            dirIterator.add(myFile);
          } else {
            if (myFile.getName().endsWith(".class")) {
              classFiles.add(myFile);
            }
          }
        }
      }
    }
    return classFiles;
  }
}
