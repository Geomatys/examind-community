/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.json.view;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * Json view filter to limit written properties.
 * 
 * @author Johann Sorel (Geomatys)
 */
public class JsonViewTest {
    
    private static class User {
        private String firstName;
        private String[] lastNames;
        private List<Project> projects;
    }
    
    private static class Project{
        @JsonIgnore
        private String uid;
        private String name;
        private Integer age;
    }
    
    private static final User CANDIDATE_1;
    private static final User CANDIDATE_2;
    static{
        CANDIDATE_1 = new User();
        CANDIDATE_1.firstName = "louis";
        CANDIDATE_1.lastNames = new String[]{"marc","jean","antoine"};
        
        final Project p1 = new Project();
        p1.uid = "AAAA1234";
        p1.name = "examind";
        p1.age = 12;
        final Project p2 = new Project();
        p2.uid = "BBBB5678";
        p2.name = "geotk";
        p2.age = 20;
        CANDIDATE_1.projects = Arrays.asList(p1,p2);
        
        
        CANDIDATE_2 = new User();
        CANDIDATE_2.firstName = "dupont";
        CANDIDATE_2.lastNames = new String[]{"laurent","emile"};
        
        final Project p3 = new Project();
        p3.uid = "CCCC1234";
        p3.name = "mapfaces";
        p3.age = 7;
        final Project p4 = new Project();
        p4.uid = "DDDD5678";
        p4.name = "mdweb";
        p4.age = 42;
        CANDIDATE_2.projects = Arrays.asList(p3,p4);
    }
    
    /**
     * Test mapping all fields.
     * 
     * @throws JsonProcessingException 
     */
    @Test
    public void viewFullTest() throws JsonProcessingException{
        final ObjectMapper mapper = new ObjectMapper();
        String str = mapper.writeValueAsString(new JsonView(CANDIDATE_1));
        Assert.assertEquals(
                "{\"firstName\":\"louis\","
                        + "\"lastNames\":[\"marc\",\"jean\",\"antoine\"],"
                        + "\"projects\":["
                            + "{\"name\":\"examind\",\"age\":12},"
                            + "{\"name\":\"geotk\",\"age\":20}]}"
                ,str);
        
    }
    
    /**
     * Test selecting a single field.
     * 
     * @throws JsonProcessingException 
     */
    @Test
    public void viewNameTest() throws JsonProcessingException{
        final ObjectMapper mapper = new ObjectMapper();
        String str = mapper.writeValueAsString(new JsonView(CANDIDATE_1, "firstName"));
        Assert.assertEquals(
                "{\"firstName\":\"louis\"}"
                ,str);
        
    }
    
    /**
     * Test selecting a children document field.
     * 
     * @throws JsonProcessingException 
     */
    @Test
    public void viewProjectAgeTest() throws JsonProcessingException{
        final ObjectMapper mapper = new ObjectMapper();
        String str = mapper.writeValueAsString(new JsonView(CANDIDATE_1, "projects.age"));
        Assert.assertEquals(
                "{\"projects\":[{\"age\":12},{\"age\":20}]}"
                ,str);
    }
    
    /**
     * Test mapping a collection of documents.
     * 
     * @throws JsonProcessingException 
     */
    @Test
    public void viewCollectionTest() throws JsonProcessingException{
        final ObjectMapper mapper = new ObjectMapper();
        
        final List<User> users = Arrays.asList(CANDIDATE_1,CANDIDATE_2);
        
        String str = mapper.writeValueAsString(new JsonView(users, "projects.age"));
        Assert.assertEquals(
                "["+
                "{\"projects\":[{\"age\":12},{\"age\":20}]},"+
                "{\"projects\":[{\"age\":7},{\"age\":42}]}"+
                "]"
                ,str);
    }
    
}
