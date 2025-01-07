package com.examind.openeo.api.rest.process.dto;

import com.examind.openeo.api.rest.dto.CheckMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.geotoolkit.atom.xml.Link;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;

import java.util.*;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Process-Discovery">OpenEO Doc</a>
 */
public class Process {

    public Process() {}

    public Process(String id, String summary, String description, List<String> categories, List<ProcessParameter> parameters,
                   ProcessReturn returns, Boolean deprecated, Boolean experimental, Map<String, ProcessExceptionInformation> exceptions,
                   List<ProcessDescription> examples, List<Link> links) {
        this.id = id;
        this.summary = summary;
        this.description = description;
        this.categories = categories;
        this.parameters = parameters;
        this.returns = returns;
        this.deprecated = deprecated;
        this.experimental = experimental;
        this.exceptions = exceptions;
        this.examples = examples;
        this.links = links;
    }

    @JsonProperty("id")
    private String id;

    @JsonProperty("summary")
    private String summary = "No summary specified";

    @JsonProperty("description")
    private String description = "No description specified";

    @JsonProperty("categories")
    private List<String> categories = new ArrayList<>();

    @JsonProperty("parameters")
    private List<ProcessParameter> parameters = new ArrayList<>();

    @JsonProperty("returns")
    private ProcessReturn returns = null;

    @JsonProperty(value = "deprecated", defaultValue = "false")
    private Boolean deprecated = false;

    @JsonProperty(value = "experimental", defaultValue = "false")
    private Boolean experimental = false;

    @JsonProperty("exceptions")
    private Map<String, ProcessExceptionInformation> exceptions = new HashMap<>();

    @JsonProperty("examples")
    private List<ProcessDescription> examples = new ArrayList<>();

    @JsonProperty("links")
    private List<Link> links = new ArrayList<>();

    @JsonProperty("process_graph")
    private Map<String, ProcessDescription> processGraph = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<ProcessParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<ProcessParameter> parameters) {
        this.parameters = parameters;
    }

    public ProcessReturn getReturns() {
        return returns;
    }

    public void setReturns(ProcessReturn returns) {
        this.returns = returns;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Boolean getExperimental() {
        return experimental;
    }

    public void setExperimental(Boolean experimental) {
        this.experimental = experimental;
    }

    public Map<String, ProcessExceptionInformation> getExceptions() {
        return exceptions;
    }

    public void setExceptions(Map<String, ProcessExceptionInformation> exceptions) {
        this.exceptions = exceptions;
    }

    public List<ProcessDescription> getExamples() {
        return examples;
    }

    public void setExamples(List<ProcessDescription> examples) {
        this.examples = examples;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public Map<String, ProcessDescription> getProcessGraph() {
        return processGraph;
    }

    public void setProcessGraph(Map<String, ProcessDescription> processGraph) {
        this.processGraph = processGraph;
    }

    public CheckMessage isProcessGraphValid(List<ProcessDescriptor> descriptors) {
        List<String> descriptorIds = descriptors.stream().map(d -> d.getIdentifier().getCode()).toList();

        for (Map.Entry<String, ProcessDescription> entry : processGraph.entrySet()) {
            ProcessDescription process = entry.getValue();

            //Process specified in the graph exist in the list of available process ?
            if (!descriptorIds.contains(process.getProcessId().split("\\.",2)[1])) {
                return new CheckMessage(false, "No available process with this id : " + process.getProcessId());
            }

            //Check if the arguments are valid (arg type, links to an existing parameter / node, ...)
            for (Map.Entry<String, ProcessDescriptionArgument> argEntry : process.getArguments().entrySet()) {
                ProcessDescriptionArgument arg = argEntry.getValue();
                CheckMessage checkMessage = isProcessDescriptionArgumentValid(arg);
                if (!checkMessage.isValid()) {
                    return checkMessage;
                }
            }

            //Checks between "real" process, and given process
            ProcessDescriptor processDescriptor = descriptors.stream()
                    .filter(descriptor -> descriptor.getIdentifier().getCode().equals(process.getProcessId().split("\\.",2)[1]))
                    .findFirst()
                    .orElse(null);

            if (processDescriptor != null) {
                if (processDescriptor.getInputDescriptor().descriptors().stream().filter(descriptor -> descriptor.getMinimumOccurs() > 0).count() > process.getArguments().size()) {
                    //The number of inputs arguments given is not the same of the number of arguments needed by the process
                    return new CheckMessage(false, "Number of arguments provided and needed by the process (" + process.getProcessId() +
                            ") are not the same (" + process.getArguments().size() + " provided) / (" + processDescriptor.getInputDescriptor().descriptors().size() + " needed)");
                }

                //Check (if possible) if the argument given is compatible with the argument needed
                for(int i=0; i<process.getArguments().size(); i++) {
                    GeneralParameterDescriptor inputDescriptor = processDescriptor.getInputDescriptor().descriptors().get(i);
                    ProcessDescriptionArgument processDescriptionArgument = process.getArguments().get(inputDescriptor.getName().getCode());
                                                                         // process.getArguments().values().stream().toList().get(i);
                    //Cannot find the needed argument in the list of passed arguments
                    if(processDescriptionArgument == null) {
                        return new CheckMessage(false, "For the process : " + process.getProcessId() + ", no argument named " + inputDescriptor.getName().getCode() + " found");
                    }

                    if (inputDescriptor instanceof DefaultParameterDescriptor<?> inputDefaultParameterDescriptor) {
                        try {
                            if (inputDefaultParameterDescriptor.getValueType() != null) {
                                Class<?> type = inputDefaultParameterDescriptor.getValueClass();

                                switch (processDescriptionArgument.getType()) {
                                    case VALUE: //Check if the value specified is conform to the process input
                                        if (!checkClassAssignation(type, processDescriptionArgument.getValue().getClass())) {
                                            //Given input type and the needed input type are not the same
                                            return new CheckMessage(false, "For the process : " + process.getProcessId() + ", the type specified for the argument : " +
                                                    inputDescriptor.getName().getCode() + " is not correct (" + type.toString() + " needed)" );
                                        } else {
                                            if (inputDefaultParameterDescriptor.getValidValues() != null && !inputDefaultParameterDescriptor.getValidValues().isEmpty()) {
                                                if (!inputDefaultParameterDescriptor.getValidValues().contains(processDescriptionArgument.getValue())) {
                                                    return new CheckMessage(false, "For the process : " + process.getProcessId() + ", the type specified for the argument : " +
                                                            inputDescriptor.getName().getCode() + " is not is the list of accepted inputs (" +
                                                            inputDefaultParameterDescriptor.getValidValues().stream().map(Object::toString).toList() + ")");
                                                }
                                            }
                                        }
                                        break;

                                    case FROM_NODE: //Check if the node output value is conform to the process input
                                        String referencedProcessId = processGraph.get((String) processDescriptionArgument.getValue()).getProcessId().split("\\.",2)[1];

                                        ProcessDescriptor referencedProcess = descriptors.stream()
                                                .filter(descriptor -> descriptor.getIdentifier().getCode().equalsIgnoreCase(referencedProcessId))
                                                .findFirst()
                                                .orElse(null);

                                        if (referencedProcess == null) {
                                            return new CheckMessage(false, "For the process : " + process.getProcessId() + ", the referenced process : " +
                                                    processDescriptionArgument.getValue() + " does not exist");
                                        }
                                        if (referencedProcess.getOutputDescriptor().descriptors().isEmpty()) {
                                            return new CheckMessage(false, "For the process : " + process.getProcessId() + ", the referenced process : " +
                                                    processDescriptionArgument.getValue() + " has no available outputs");
                                        }

                                        //Get the first output as openEO works with processes with one output
                                        GeneralParameterDescriptor outputDescriptor = referencedProcess.getOutputDescriptor().descriptors().get(0);
                                        if (outputDescriptor instanceof DefaultParameterDescriptor<?> outputDefaultParameterDescriptor) {
                                            if (!checkClassAssignation(type, outputDefaultParameterDescriptor.getValueClass())) {
                                                return new CheckMessage(false, "For the process : " + process.getProcessId() + ", the type of the output of the referenced process : " +
                                                        processDescriptionArgument.getValue() + " is not compatible with the type of the argument : " +
                                                        inputDescriptor.getName().getCode());
                                            }
                                        } else { //TODO: Check other type of parameters
                                            return new CheckMessage(false, "Parameter type is not a 'DefaultParameterDescriptor'");
                                        }

                                        break;

                                    case FROM_PARAMETER: //Check if the parameter value is conform to the process input
                                        ProcessParameter referencedParameter = this.getParameters().stream()
                                                .filter(parameter -> parameter.getName().equalsIgnoreCase((String) processDescriptionArgument.getValue()))
                                                .findFirst()
                                                .orElse(null);

                                        if (referencedParameter == null) {
                                            return new CheckMessage(false, "For the process : " + process.getProcessId() + ", the referenced parameter : " +
                                                    processDescriptionArgument.getValue() + " does not exist");
                                        }
//                                        if (!checkClassAssignation(type, referencedParameter.getSchema().getType())) {
//                                            return new CheckMessage(false, "For the process : " + process.getProcessId() + ", the type of the referenced parameter : " +
//                                                    processDescriptionArgument.getFromParameter() + " is not compatible with the type of the argument : " +
//                                                    inputDescriptor.getName().getCode());
//                                        }
                                        break;

                                    case ARRAY: //TODO: Be compatible when examind supports combination of outputs in an array
                                        if(type.isArray()) {
                                            for(var nestedProcess : (List<ProcessDescriptionArgument>) processDescriptionArgument.getValue()) {
                                                if (nestedProcess.getType() != ProcessDescriptionArgument.ArgumentType.VALUE) {
                                                    return new CheckMessage(false, "For the process : " + process.getProcessId() + ", the content of the array for the argument : " +
                                                            inputDescriptor.getName().getCode() + " is not correct. For the moment, we only support arrays with constant values");
                                                }
                                            }

//                                            if (!checkClassAssignation(type, processDescriptionArgument.getValue().getClass())) {
//                                                return new CheckMessage(false, "For the process : " + process.getProcessId() + ", the type specified for the argument : " +
//                                                        inputDescriptor.getName().getCode() + " is not correct (this argument doesn't accept the type of array specified)");
//                                            }
                                        } else {
                                            return new CheckMessage(false, "For the process : " + process.getProcessId() + ", the type specified for the argument : " +
                                                    inputDescriptor.getName().getCode() + " is not correct (this argument is not an array)");
                                        }
                                        break;

                                    case PROCESS_GRAPH: //TODO: Support sub-process graph
                                        return new CheckMessage(false, "Sub process graph is not supported for the moment by this implementation");
                                }
                            }

                        } catch (NullPointerException ex) {
                            //Do nothing, but I need to add this catch otherwise, sometimes, even if we check if it's null, this exception is raised
                        }
                    } else { //TODO: Check other type of parameters
                        return new CheckMessage(false, "Parameter type is not a 'DefaultParameterDescriptor'");
                    }
                }

                //TODO: Maybe, add a check for the outputs ?
            } else {
                return new CheckMessage(false, "No available process with this id : " + process.getProcessId());
            }
        }

        return new CheckMessage(true, null);
    }

    private CheckMessage isProcessDescriptionArgumentValid(ProcessDescriptionArgument arg) {

        if (arg.getType() == ProcessDescriptionArgument.ArgumentType.FROM_NODE) {
            String fromNode = arg.getValue().toString();
            if (processGraph.containsKey(fromNode)) {
                return new CheckMessage(true, null);
            } else {
                return new CheckMessage(false, "Argument 'from_node' (" + fromNode + ") is not present in the process graph (no process with this name)");
            }
        }

        if (arg.getType() == ProcessDescriptionArgument.ArgumentType.FROM_PARAMETER) {
            String fromParameter = arg.getValue().toString();
            boolean found = false;
            for (ProcessParameter param : parameters) {
                if (param.getName().equals(fromParameter)) {
                    found = true;
                    break;
                }
            }
            if (found){
                return new CheckMessage(true, null);
            } else {
                return new CheckMessage(false, "Argument 'from_parameter' (" + fromParameter + ") is not present in the parameters list (no parameter with this name)");
            }
        }

        if (arg.getType() == ProcessDescriptionArgument.ArgumentType.ARRAY) {
            List<ProcessDescriptionArgument> nestedArgs = (List<ProcessDescriptionArgument>) arg.getValue();
            for (ProcessDescriptionArgument nestedArg : nestedArgs) {
                CheckMessage checkMessage = isProcessDescriptionArgumentValid(nestedArg);
                if (!checkMessage.isValid()) {
                    return checkMessage;
                }
            }
            return new CheckMessage(true, null);
        }

        return new CheckMessage(true, null);
    }

    private boolean checkClassAssignation(Class<?> needed, Class<?> provided) {
        if (needed.getName().contains("Envelope") && provided.getName().contains("BoundingBox")) {
            return true;
        }

        return needed.isAssignableFrom(provided);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Process process = (Process) o;
        return Objects.equals(id, process.id) && Objects.equals(summary, process.summary) && Objects.equals(description, process.description)
                && Objects.equals(categories, process.categories) && Objects.equals(parameters, process.parameters)
                && Objects.equals(returns, process.returns) && Objects.equals(deprecated, process.deprecated)
                && Objects.equals(experimental, process.experimental) && Objects.equals(exceptions, process.exceptions)
                && Objects.equals(examples, process.examples) && Objects.equals(links, process.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, summary, description, categories, parameters, returns, deprecated, experimental, exceptions, examples, links);
    }

    @Override
    public String toString() {
        return "Process{" +
                "id='" + id + '\'' +
                ", summary='" + summary + '\'' +
                ", description='" + description + '\'' +
                ", categories=" + categories +
                ", parameters=" + parameters +
                ", returns=" + returns +
                ", deprecated=" + deprecated +
                ", experimental=" + experimental +
                ", exceptions=" + exceptions +
                ", examples=" + examples +
                ", links=" + links +
                '}';
    }
}
