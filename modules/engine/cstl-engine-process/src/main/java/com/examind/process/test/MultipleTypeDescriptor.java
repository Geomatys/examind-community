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
package com.examind.process.test;

import java.io.File;
import java.nio.file.Path;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MultipleTypeDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "test.multiple.type";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Test multiple types.");

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    public static final String DOUBLE_ARRAY_INPUT_NAME = "double.array.input";
    private static final String DOUBLE_ARRAY_INPUT_REMARKS = "Double array input.";
    public static final ParameterDescriptor<Double[]> DOUBLE_ARRAY_INPUT = BUILDER
            .addName(DOUBLE_ARRAY_INPUT_NAME)
            .setRemarks(DOUBLE_ARRAY_INPUT_REMARKS)
            .setRequired(false)
            .create(Double[].class, null);

    public static final String DOUBLE_PRIM_ARRAY_INPUT_NAME = "double.prim.array.input";
    private static final String DOUBLE_PRIM_ARRAY_INPUT_REMARKS = "Double primitive array input.";
    public static final ParameterDescriptor<double[]> DOUBLE_PRIM_ARRAY_INPUT = BUILDER
            .addName(DOUBLE_PRIM_ARRAY_INPUT_NAME)
            .setRemarks(DOUBLE_PRIM_ARRAY_INPUT_REMARKS)
            .setRequired(false)
            .create(double[].class, null);

    public static final String DOUBLE_MULTIPLE_INPUT_NAME = "double.multiple.input";
    private static final String DOUBLE_MULTIPLE_INPUT_REMARKS = "Double multiple input.";
    public static final ParameterDescriptor<Double> DOUBLE_MULTIPLE_INPUT 
            = new ExtendedParameterDescriptor<>(
                DOUBLE_MULTIPLE_INPUT_NAME, DOUBLE_MULTIPLE_INPUT_REMARKS, 0, Integer.MAX_VALUE, Double.class, null, null, null);

    public static final String INTEGER_ARRAY_INPUT_NAME = "integer.array.input";
    private static final String INTEGER_ARRAY_INPUT_REMARKS = "Double array input.";
    public static final ParameterDescriptor<Integer[]> INTEGER_ARRAY_INPUT = BUILDER
            .addName(INTEGER_ARRAY_INPUT_NAME)
            .setRemarks(INTEGER_ARRAY_INPUT_REMARKS)
            .setRequired(false)
            .create(Integer[].class, null);

    public static final String INTEGER_PRIM_ARRAY_INPUT_NAME = "integer.prim.array.input";
    private static final String INTEGER_PRIM_ARRAY_INPUT_REMARKS = "Integer primitive array input.";
    public static final ParameterDescriptor<int[]> INTEGER_PRIM_ARRAY_INPUT = BUILDER
            .addName(INTEGER_PRIM_ARRAY_INPUT_NAME)
            .setRemarks(INTEGER_PRIM_ARRAY_INPUT_REMARKS)
            .setRequired(false)
            .create(int[].class, null);

    public static final String INTEGER_MULTIPLE_INPUT_NAME = "integer.multiple.input";
    private static final String INTEGER_MULTIPLE_INPUT_REMARKS = "Integer multiple input.";
    public static final ParameterDescriptor<Integer> INTEGER_MULTIPLE_INPUT
            = new ExtendedParameterDescriptor<>(
                INTEGER_MULTIPLE_INPUT_NAME, INTEGER_MULTIPLE_INPUT_REMARKS, 0, Integer.MAX_VALUE, Integer.class, null, null, null);
           

    public static final String BOOLEAN_ARRAY_INPUT_NAME = "boolean.array.input";
    private static final String BOOLEAN_ARRAY_INPUT_REMARKS = "Boolean array input.";
    public static final ParameterDescriptor<Boolean[]> BOOLEAN_ARRAY_INPUT = BUILDER
            .addName(BOOLEAN_ARRAY_INPUT_NAME)
            .setRemarks(BOOLEAN_ARRAY_INPUT_REMARKS)
            .setRequired(false)
            .create(Boolean[].class, null);

    public static final String BOOLEAN_PRIM_ARRAY_INPUT_NAME = "boolean.prim.array.input";
    private static final String BOOLEAN_PRIM_ARRAY_INPUT_REMARKS = "Boolean primitive array input.";
    public static final ParameterDescriptor<boolean[]> BOOLEAN_PRIM_ARRAY_INPUT = BUILDER
            .addName(BOOLEAN_PRIM_ARRAY_INPUT_NAME)
            .setRemarks(BOOLEAN_PRIM_ARRAY_INPUT_REMARKS)
            .setRequired(false)
            .create(boolean[].class, null);

    public static final String BOOLEAN_MULTIPLE_INPUT_NAME = "boolean.multiple.input";
    private static final String BOOLEAN_MULTIPLE_INPUT_REMARKS = "Boolean multiple input.";
    public static final ParameterDescriptor<Boolean> BOOLEAN_MULTIPLE_INPUT
            = new ExtendedParameterDescriptor<>(
                BOOLEAN_MULTIPLE_INPUT_NAME, BOOLEAN_MULTIPLE_INPUT_REMARKS, 0, Integer.MAX_VALUE, Boolean.class, null, null, null);



    public static final String LONG_ARRAY_INPUT_NAME = "long.array.input";
    private static final String LONG_ARRAY_INPUT_REMARKS = "Long array input.";
    public static final ParameterDescriptor<Long[]> LONG_ARRAY_INPUT = BUILDER
            .addName(LONG_ARRAY_INPUT_NAME)
            .setRemarks(LONG_ARRAY_INPUT_REMARKS)
            .setRequired(false)
            .create(Long[].class, null);

    public static final String LONG_PRIM_ARRAY_INPUT_NAME = "long.prim.array.input";
    private static final String LONG_PRIM_ARRAY_INPUT_REMARKS = "Long primitive array input.";
    public static final ParameterDescriptor<long[]> LONG_PRIM_ARRAY_INPUT = BUILDER
            .addName(LONG_PRIM_ARRAY_INPUT_NAME)
            .setRemarks(LONG_PRIM_ARRAY_INPUT_REMARKS)
            .setRequired(false)
            .create(long[].class, null);

    public static final String LONG_MULTIPLE_INPUT_NAME = "long.multiple.input";
    private static final String LONG_MULTIPLE_INPUT_REMARKS = "Long multiple input.";
    public static final ParameterDescriptor<Long> LONG_MULTIPLE_INPUT
            = new ExtendedParameterDescriptor<>(
                LONG_MULTIPLE_INPUT_NAME, LONG_MULTIPLE_INPUT_REMARKS, 0, Integer.MAX_VALUE, Long.class, null, null, null);

    public static final String CHAR_ARRAY_INPUT_NAME = "char.array.input";
    private static final String CHAR_ARRAY_INPUT_REMARKS = "character array input.";
    public static final ParameterDescriptor<Character[]> CHAR_ARRAY_INPUT = BUILDER
            .addName(CHAR_ARRAY_INPUT_NAME)
            .setRemarks(CHAR_ARRAY_INPUT_REMARKS)
            .setRequired(false)
            .create(Character[].class, null);

    public static final String CHAR_PRIM_ARRAY_INPUT_NAME = "char.prim.array.input";
    private static final String CHAR_PRIM_ARRAY_INPUT_REMARKS = "Character primitive array input.";
    public static final ParameterDescriptor<char[]> CHAR_PRIM_ARRAY_INPUT = BUILDER
            .addName(CHAR_PRIM_ARRAY_INPUT_NAME)
            .setRemarks(CHAR_PRIM_ARRAY_INPUT_REMARKS)
            .setRequired(false)
            .create(char[].class, null);

    public static final String CHAR_MULTIPLE_INPUT_NAME = "char.multiple.input";
    private static final String CHAR_MULTIPLE_INPUT_REMARKS = "Character multiple input.";
    public static final ParameterDescriptor<Character> CHAR_MULTIPLE_INPUT
            = new ExtendedParameterDescriptor<>(
                CHAR_MULTIPLE_INPUT_NAME, CHAR_MULTIPLE_INPUT_REMARKS, 0, Integer.MAX_VALUE, Character.class, null, null, null);

    public static final String FILE_INPUT_NAME = "file.input";
    private static final String FILE_INPUT_REMARKS = "File input.";
    public static final ParameterDescriptor<File> FILE_INPUT = BUILDER
            .addName(FILE_INPUT_NAME)
            .setRemarks(FILE_INPUT_REMARKS)
            .setRequired(false)
            .create(File.class, null);

    public static final String PATH_INPUT_NAME = "path.input";
    private static final String PATH_INPUT_REMARKS = "Path input.";
    public static final ParameterDescriptor<Path> PATH_INPUT = BUILDER
            .addName(PATH_INPUT_NAME)
            .setRemarks(PATH_INPUT_REMARKS)
            .setRequired(false)
            .create(Path.class, null);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(DOUBLE_ARRAY_INPUT, DOUBLE_PRIM_ARRAY_INPUT, DOUBLE_MULTIPLE_INPUT, 
                         INTEGER_ARRAY_INPUT, INTEGER_PRIM_ARRAY_INPUT, INTEGER_MULTIPLE_INPUT,
                         LONG_ARRAY_INPUT, LONG_PRIM_ARRAY_INPUT, LONG_MULTIPLE_INPUT,
                         BOOLEAN_ARRAY_INPUT, BOOLEAN_PRIM_ARRAY_INPUT, BOOLEAN_MULTIPLE_INPUT,
                         CHAR_ARRAY_INPUT, CHAR_PRIM_ARRAY_INPUT, CHAR_MULTIPLE_INPUT,
                         FILE_INPUT, PATH_INPUT);


    public static final String DOUBLE_PRIM_ARRAY_OUTPUT_NAME = "double.prim.array.output";
    private static final String DOUBLE_PRIM_ARRAY_OUTPUT_REMARKS = "Double primitive array output.";
    public static final ParameterDescriptor<double[]> DOUBLE_PRIM_ARRAY_OUTPUT = BUILDER
            .addName(DOUBLE_PRIM_ARRAY_OUTPUT_NAME)
            .setRemarks(DOUBLE_PRIM_ARRAY_OUTPUT_REMARKS)
            .setRequired(false)
            .create(double[].class, null);

    public static final String DOUBLE_ARRAY_OUTPUT_NAME = "double.array.output";
    private static final String DOUBLE_ARRAY_OUTPUT_REMARKS = "Double array output.";
    public static final ParameterDescriptor<Double[]> DOUBLE_ARRAY_OUTPUT = BUILDER
            .addName(DOUBLE_ARRAY_OUTPUT_NAME)
            .setRemarks(DOUBLE_ARRAY_OUTPUT_REMARKS)
            .setRequired(false)
            .create(Double[].class, null);

    public static final String DOUBLE_MULTIPLE_OUTPUT_NAME = "double.multiple.output";
    private static final String DOUBLE_MULTIPLE_OUTPUT_REMARKS = "Double multiple output.";
    public static final ParameterDescriptor<Double> DOUBLE_MULTIPLE_OUTPUT
            = new ExtendedParameterDescriptor<>(
                DOUBLE_MULTIPLE_OUTPUT_NAME, DOUBLE_MULTIPLE_OUTPUT_REMARKS, 0, Integer.MAX_VALUE, Double.class, null, null, null);

    public static final String INTEGER_PRIM_ARRAY_OUTPUT_NAME = "integer.prim.array.output";
    private static final String INTEGER_PRIM_ARRAY_OUTPUT_REMARKS = "Integer primitive array output.";
    public static final ParameterDescriptor<int[]> INTEGER_PRIM_ARRAY_OUTPUT = BUILDER
            .addName(INTEGER_PRIM_ARRAY_OUTPUT_NAME)
            .setRemarks(INTEGER_PRIM_ARRAY_OUTPUT_REMARKS)
            .setRequired(false)
            .create(int[].class, null);

    public static final String INTEGER_ARRAY_OUTPUT_NAME = "integer.array.output";
    private static final String INTEGER_ARRAY_OUTPUT_REMARKS = "Integer array output.";
    public static final ParameterDescriptor<Integer[]> INTEGER_ARRAY_OUTPUT = BUILDER
            .addName(INTEGER_ARRAY_OUTPUT_NAME)
            .setRemarks(INTEGER_ARRAY_OUTPUT_REMARKS)
            .setRequired(false)
            .create(Integer[].class, null);

    public static final String INTEGER_MULTIPLE_OUTPUT_NAME = "integer.multiple.output";
    private static final String INTEGER_MULTIPLE_OUTPUT_REMARKS = "Integer multiple output.";
    public static final ParameterDescriptor<Integer> INTEGER_MULTIPLE_OUTPUT
            = new ExtendedParameterDescriptor<>(
                INTEGER_MULTIPLE_OUTPUT_NAME, INTEGER_MULTIPLE_OUTPUT_REMARKS, 0, Integer.MAX_VALUE, Integer.class, null, null, null);
    

    public static final String BOOLEAN_PRIM_ARRAY_OUTPUT_NAME = "boolean.prim.array.output";
    private static final String BOOLEAN_PRIM_ARRAY_OUTPUT_REMARKS = "Boolean primitive array output.";
    public static final ParameterDescriptor<boolean[]> BOOLEAN_PRIM_ARRAY_OUTPUT = BUILDER
            .addName(BOOLEAN_PRIM_ARRAY_OUTPUT_NAME)
            .setRemarks(BOOLEAN_PRIM_ARRAY_OUTPUT_REMARKS)
            .setRequired(false)
            .create(boolean[].class, null);

    public static final String BOOLEAN_ARRAY_OUTPUT_NAME = "boolean.array.output";
    private static final String BOOLEAN_ARRAY_OUTPUT_REMARKS = "Boolean array output.";
    public static final ParameterDescriptor<Boolean[]> BOOLEAN_ARRAY_OUTPUT = BUILDER
            .addName(BOOLEAN_ARRAY_OUTPUT_NAME)
            .setRemarks(BOOLEAN_ARRAY_OUTPUT_REMARKS)
            .setRequired(false)
            .create(Boolean[].class, null);

    public static final String BOOLEAN_MULTIPLE_OUTPUT_NAME = "boolean.multiple.output";
    private static final String BOOLEAN_MULTIPLE_OUTPUT_REMARKS = "Boolean multiple output.";
    public static final ParameterDescriptor<Boolean> BOOLEAN_MULTIPLE_OUTPUT
            = new ExtendedParameterDescriptor<>(
                BOOLEAN_MULTIPLE_OUTPUT_NAME, BOOLEAN_MULTIPLE_OUTPUT_REMARKS, 0, Integer.MAX_VALUE, Boolean.class, null, null, null);
    

    public static final String LONG_PRIM_ARRAY_OUTPUT_NAME = "long.prim.array.output";
    private static final String LONG_PRIM_ARRAY_OUTPUT_REMARKS = "Long primitive array output.";
    public static final ParameterDescriptor<long[]> LONG_PRIM_ARRAY_OUTPUT = BUILDER
            .addName(LONG_PRIM_ARRAY_OUTPUT_NAME)
            .setRemarks(LONG_PRIM_ARRAY_OUTPUT_REMARKS)
            .setRequired(false)
            .create(long[].class, null);

    public static final String LONG_ARRAY_OUTPUT_NAME = "long.array.output";
    private static final String LONG_ARRAY_OUTPUT_REMARKS = "Long array output.";
    public static final ParameterDescriptor<Long[]> LONG_ARRAY_OUTPUT = BUILDER
            .addName(LONG_ARRAY_OUTPUT_NAME)
            .setRemarks(LONG_ARRAY_OUTPUT_REMARKS)
            .setRequired(false)
            .create(Long[].class, null);

    public static final String LONG_MULTIPLE_OUTPUT_NAME = "long.multiple.output";
    private static final String LONG_MULTIPLE_OUTPUT_REMARKS = "Long multiple output.";
    public static final ParameterDescriptor<Long> LONG_MULTIPLE_OUTPUT
            = new ExtendedParameterDescriptor<>(
                LONG_MULTIPLE_OUTPUT_NAME, LONG_MULTIPLE_OUTPUT_REMARKS, 0, Integer.MAX_VALUE, Long.class, null, null, null);


    public static final String CHAR_PRIM_ARRAY_OUTPUT_NAME = "char.prim.array.output";
    private static final String CHAR_PRIM_ARRAY_OUTPUT_REMARKS = "Character primitive array output.";
    public static final ParameterDescriptor<char[]> CHAR_PRIM_ARRAY_OUTPUT = BUILDER
            .addName(CHAR_PRIM_ARRAY_OUTPUT_NAME)
            .setRemarks(CHAR_PRIM_ARRAY_OUTPUT_REMARKS)
            .setRequired(false)
            .create(char[].class, null);

    public static final String CHAR_ARRAY_OUTPUT_NAME = "char.array.output";
    private static final String CHAR_ARRAY_OUTPUT_REMARKS = "Character array output.";
    public static final ParameterDescriptor<Character[]> CHAR_ARRAY_OUTPUT = BUILDER
            .addName(CHAR_ARRAY_OUTPUT_NAME)
            .setRemarks(CHAR_ARRAY_OUTPUT_REMARKS)
            .setRequired(false)
            .create(Character[].class, null);

    public static final String CHAR_MULTIPLE_OUTPUT_NAME = "char.multiple.output";
    private static final String CHAR_MULTIPLE_OUTPUT_REMARKS = "Character multiple output.";
    public static final ParameterDescriptor<Character> CHAR_MULTIPLE_OUTPUT
            = new ExtendedParameterDescriptor<>(
                CHAR_MULTIPLE_OUTPUT_NAME, CHAR_MULTIPLE_OUTPUT_REMARKS, 0, Integer.MAX_VALUE, Character.class, null, null, null);

    public static final String FILE_OUTPUT_NAME = "file.output";
    private static final String FILE_OUTPUT_REMARKS = "File output.";
    public static final ParameterDescriptor<File> FILE_OUTPUT = BUILDER
            .addName(FILE_OUTPUT_NAME)
            .setRemarks(FILE_OUTPUT_REMARKS)
            .setRequired(false)
            .create(File.class, null);

    public static final String PATH_OUTPUT_NAME = "path.output";
    private static final String PATH_OUTPUT_REMARKS = "Path output.";
    public static final ParameterDescriptor<Path> PATH_OUTPUT = BUILDER
            .addName(PATH_OUTPUT_NAME)
            .setRemarks(PATH_OUTPUT_REMARKS)
            .setRequired(false)
            .create(Path.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(DOUBLE_ARRAY_OUTPUT, DOUBLE_PRIM_ARRAY_OUTPUT, DOUBLE_MULTIPLE_OUTPUT, 
                         INTEGER_ARRAY_OUTPUT, INTEGER_PRIM_ARRAY_OUTPUT, INTEGER_MULTIPLE_OUTPUT,
                         LONG_ARRAY_OUTPUT, LONG_PRIM_ARRAY_OUTPUT, LONG_MULTIPLE_OUTPUT,
                         BOOLEAN_ARRAY_OUTPUT, BOOLEAN_PRIM_ARRAY_OUTPUT, BOOLEAN_MULTIPLE_OUTPUT,
                         CHAR_ARRAY_OUTPUT, CHAR_PRIM_ARRAY_OUTPUT, CHAR_MULTIPLE_OUTPUT,
                         FILE_OUTPUT, PATH_OUTPUT);

    public static final ProcessDescriptor INSTANCE = new MultipleTypeDescriptor();

    public MultipleTypeDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new MultipleTypeProcess(this, input);
    }

}
