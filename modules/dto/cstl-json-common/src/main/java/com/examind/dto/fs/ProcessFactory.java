/*
 *    Examind community - An open source and standard compliant SDI
 *
 * Copyright 2025 Geomatys.
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
package com.examind.dto.fs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Guilhem Legal
 */
public class ProcessFactory {
    
    private String authority;
    
    private List<String> process;

    /**
     * @return the authority
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * @param authority the authority to set
     */
    public void setAuthority(String authority) {
        this.authority = authority;
    }

    /**
     * @return the process
     */
    public List<String> getProcess() {
        if (process == null) {
            this.process = new ArrayList<>();
        }
        return process;
    }

    /**
     * @param process the process to set
     */
    public void setProcess(List<String> process) {
        this.process = process;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (authority != null) {
            sb.append("authority:").append(authority).append("\n");
        }
        if (process != null) {
            sb.append("process:\n");
            for (String col : process) {
                sb.append(" - ").append(col).append("\n");
            }
        }
        return sb.toString();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        
        if (obj instanceof ProcessFactory that) {
            return Objects.equals(this.authority, that.authority) &&
                   Objects.equals(this.process, that.process);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.authority);
        hash = 83 * hash + Objects.hashCode(this.process);
        return hash;
    }
}
