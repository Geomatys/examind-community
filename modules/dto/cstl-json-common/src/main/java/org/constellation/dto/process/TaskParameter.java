/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.dto.process;

import java.util.Objects;
import org.constellation.dto.Identifiable;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class TaskParameter extends Identifiable {

    private Integer owner;
    private String name;
    private Long date;
    private String processAuthority;
    private String processCode;
    private String inputs;
    private String trigger;
    private String triggerType;
    private String type;

    public TaskParameter() {
    }

    public TaskParameter(Integer id, Integer owner, String name, Long date,
            String processAuthority, String processCode, String inputs,
            String trigger, String triggerType, String type) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.date = date;
        this.processAuthority = processAuthority;
        this.processCode = processCode;
        this.inputs = inputs;
        this.trigger = trigger;
        this.triggerType = triggerType;
        this.type = type;
    }

    /**
     * @return the owner
     */
    public Integer getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Integer owner) {
        this.owner = owner;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the date
     */
    public Long getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(Long date) {
        this.date = date;
    }

    /**
     * @return the processAuthority
     */
    public String getProcessAuthority() {
        return processAuthority;
    }

    /**
     * @param processAuthority the processAuthority to set
     */
    public void setProcessAuthority(String processAuthority) {
        this.processAuthority = processAuthority;
    }

    /**
     * @return the processCode
     */
    public String getProcessCode() {
        return processCode;
    }

    /**
     * @param processCode the processCode to set
     */
    public void setProcessCode(String processCode) {
        this.processCode = processCode;
    }

    /**
     * @return the inputs
     */
    public String getInputs() {
        return inputs;
    }

    /**
     * @param inputs the inputs to set
     */
    public void setInputs(String inputs) {
        this.inputs = inputs;
    }

    /**
     * @return the trigger
     */
    public String getTrigger() {
        return trigger;
    }

    /**
     * @param trigger the trigger to set
     */
    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    /**
     * @return the triggerType
     */
    public String getTriggerType() {
        return triggerType;
    }

    /**
     * @param triggerType the triggerType to set
     */
    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            final TaskParameter that = (TaskParameter) obj;
            return Objects.equals(this.date, that.date) &&
                   Objects.equals(this.id, that.id) &&
                   Objects.equals(this.inputs, that.inputs) &&
                   Objects.equals(this.name, that.name) &&
                   Objects.equals(this.owner, that.owner) &&
                   Objects.equals(this.processAuthority, that.processAuthority) &&
                   Objects.equals(this.processCode, that.processCode) &&
                   Objects.equals(this.trigger, that.trigger) &&
                   Objects.equals(this.triggerType, that.triggerType) &&
                   Objects.equals(this.type, that.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.owner);
        hash = 71 * hash + Objects.hashCode(this.name);
        hash = 71 * hash + Objects.hashCode(this.date);
        hash = 71 * hash + Objects.hashCode(this.processAuthority);
        hash = 71 * hash + Objects.hashCode(this.processCode);
        hash = 71 * hash + Objects.hashCode(this.inputs);
        hash = 71 * hash + Objects.hashCode(this.trigger);
        hash = 71 * hash + Objects.hashCode(this.triggerType);
        hash = 71 * hash + Objects.hashCode(this.type);
        return hash;
    }



    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TaskParameter{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", date='").append(date).append('\'');
        sb.append(", owner='").append(owner).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", inputs='").append(inputs).append('\'');
        sb.append(", processAuthority='").append(processAuthority).append('\'');
        sb.append(", processCode='").append(processCode).append('\'');
        sb.append(", trigger='").append(trigger).append('\'');
        sb.append(", triggerType='").append(triggerType).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
