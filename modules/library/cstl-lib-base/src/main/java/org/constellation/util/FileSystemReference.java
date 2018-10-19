/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.constellation.util;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileSystemReference {

        public final String scheme;
        public final String uri;
        public final FileSystem fs;
        private final Set<Integer> dsRef = new HashSet<>();
        private final AtomicInteger unknowRef = new AtomicInteger(0);

        public FileSystemReference(String scheme, String uri, FileSystem fs) {
            this.scheme = scheme;
            this.fs = fs;
            this.uri = uri;
        }

        public void addDsRef(Integer dsId) {
            dsRef.add(dsId);
        }

        public void addUnknowRef() {
            unknowRef.incrementAndGet();
        }

        public boolean closeFs(Integer dsId) throws IOException {
            dsRef.remove(dsId);
            if (dsRef.isEmpty()) {
                fs.close();
                return true;
            }
            return false;
        }

        public boolean close() throws IOException {
            int nbRef = unknowRef.decrementAndGet();
            if (nbRef == 0) {
                fs.close();
                return true;
            }
            return false;
        }
    }
