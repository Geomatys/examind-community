/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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
package org.constellation.api.rest;


import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.Deflater;
import java.util.zip.ZipOutputStream;
import javax.activation.MimetypesFileTypeMap;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.dto.metadata.Attachment;
import org.constellation.dto.CstlUser;
import org.constellation.dto.DataBrief;
import org.constellation.dto.Filter;
import org.constellation.dto.Page;
import org.constellation.dto.PagedSearch;
import org.constellation.dto.Search;
import org.constellation.dto.Sort;
import org.constellation.dto.metadata.GroupStatBrief;
import org.constellation.dto.metadata.MetadataBrief;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.dto.metadata.OwnerStatBrief;
import org.constellation.dto.metadata.Profile;
import org.constellation.dto.metadata.RootObj;
import org.constellation.dto.metadata.User;
import org.constellation.dto.metadata.ValidationList;
import org.constellation.exception.ConfigurationException;
import org.constellation.json.metadata.Template;
import org.constellation.json.metadata.bean.TemplateResolver;
import org.constellation.metadata.utils.Utils;
import org.constellation.util.Util;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.nio.ZipUtilities;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.http.HttpStatus.*;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.*;
import org.springframework.web.bind.annotation.RequestMethod;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Provides all necessary methods to serve rest api for metadata.
 * Used by the new metadata dashboard page.
 *
 * @author Mehdi Sidhoum (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
@RestController
public class MetadataRestAPI extends AbstractRestAPI{

    private static final MimetypesFileTypeMap MIME_TYPE_MAP = new MimetypesFileTypeMap();
    static {
        MIME_TYPE_MAP.addMimeTypes("image/png png PNG");
    }


    /**
     * Inject metadata business
     */
    @Inject
    private IMetadataBusiness metadataBusiness;

    /**
     * Inject configuration business
     */
    @Inject
    private IConfigurationBusiness configurationBusiness;

    @Inject
    private IDataBusiness dataBusiness;

    @Inject
    private TemplateResolver templateResolver;

    public MetadataRestAPI() {}

    /**
     * Returns the list of profiles used by metadata.
     * the list contains only profiles used, not all.
     *
     * @param type Filter on metadata type (DOC, MODELE, CONTACT,...)
     * @param all flag to return only the used profiles or all.
     * @param dataType Filter on data type supported by the profiles (optional).
     *
     * @return List of {@link Profile}
     */
    @RequestMapping(value="/profiles",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getProfiles(@RequestParam(name = "type", required = false) final String type,
                                      @RequestParam(name = "all", defaultValue = "true") final boolean all,
                                      @RequestParam(name = "dataType", required = false) final String dataType) {
        try {
            final List<Profile> result = new ArrayList<>();
            if (all) {
                final List<String> allProfiles = metadataBusiness.getProfilesMatchingType(dataType);
                for (final String p : allProfiles) {
                    result.add(new Profile(p, 0));
                }
            } else {
                final Map<String, Object> filterMap = new HashMap<>();
                if (type != null) {
                    filterMap.put("type", type);
                }
                final Map<String, Integer> map = metadataBusiness.getProfilesCount(filterMap, dataType);
                if (map != null) {
                    for (final Map.Entry<String, Integer> entry : map.entrySet()) {
                        result.add(new Profile(entry.getKey(), entry.getValue()));
                    }
                }
            }
            return new ResponseEntity(result, OK);
        }catch(Throwable ex) {
            LOGGER.log(Level.WARNING,"Cannot list metadata metadata profiles due to exception error : "+ ex.getLocalizedMessage());
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Returns the list of users to serve metadata owner property.
     * since the UserRest api does not work yet,
     * we use this method to serve the list of users.
     *
     * @return List of {@link User}
     */
    @RequestMapping(value="/metadatas/users",method=GET,produces=APPLICATION_JSON_VALUE)
    public List<User> getUsersList() {
        return metadataBusiness.getUsers();
    }

    /**
     * Proceed to get list of records {@link MetadataBrief} in Page object for dashboard.
     * the list can be filtered, sorted and use the pagination.
     *
     * @param pagedSearch given params of filters, sorting and pagination served by a pojo {link PagedSearch}
     * @param type type filter on metadata (DOC, MODELE, CONTACT,...)
     * @param req the http request needed to get the current user.
     *
     * @return {@link Page} of {@link MetadataBrief}
     */
    @RequestMapping(value="/metadatas/search",method=POST,produces=APPLICATION_JSON_VALUE)
    public Page<MetadataBrief> search(@RequestParam(name = "type", defaultValue = "DOC") final String type,
            @RequestBody final PagedSearch pagedSearch,
            final HttpServletRequest req) {

        //filters
        final Map<String,Object> filterMap = prepareFilters(pagedSearch,req);
        filterMap.put("type", type);

        //sorting
        final Sort sort = pagedSearch.getSort();
        Map.Entry<String,String> sortEntry = null;
        if (sort != null) {
            sortEntry = new AbstractMap.SimpleEntry<>(sort.getField(),sort.getOrder().toString());
        }

        //pagination
        final int pageNumber = pagedSearch.getPage();
        final int rowsPerPage = pagedSearch.getSize();

        final Map.Entry<Integer,List<MetadataBrief>> entry = metadataBusiness.filterAndGetBrief(filterMap,sortEntry,pageNumber,rowsPerPage);
        final int total = entry.getKey();
        final List<MetadataBrief> results = entry.getValue();

        // Build and return the content list of page.
        return new Page<MetadataBrief>()
                .setNumber(pageNumber)
                .setSize(rowsPerPage)
                .setContent(results)
                .setTotal(total);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Entry<String, Object> transformFilter(Filter f, final HttpServletRequest req) {
        Map.Entry<String, Object> result = super.transformFilter(f, req);
        if (result != null) {
            return result;
        }
        String value = f.getValue();
        if (value == null || "_all".equals(value)) {
            return null;
        }
        if ("group".equals(f.getField())) {
            try {
                final int groupId = Integer.valueOf(value);
                return new AbstractMap.SimpleEntry<>("group", groupId);
            } catch (Exception ex) {
                //try for case of current user's group
                if ("_mygroup".equals(value)) {
                    //try to find the user's group from login
                    final String login = req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : null;
                    final Optional<CstlUser> optUser = userRepository.findOne(login);
                    if (optUser != null && optUser.isPresent()) {
                        final CstlUser user = optUser.get();
                        if (user != null) {
                            final User pojoUser = metadataBusiness.getUser(user.getId());
                            if (pojoUser != null && pojoUser.getGroup() != null) {
                                return new AbstractMap.SimpleEntry<>("group", pojoUser.getGroup().getId());
                            }
                        }
                    }
                }
                return null;
            }
        } else if ("validated".equals(f.getField()) || "published".equals(f.getField()) || "isShared".equals(f.getField())) {

            return new AbstractMap.SimpleEntry<>(f.getField(), Boolean.parseBoolean(value));

        } else if ("parent".equals(f.getField())) {
            try {
                final int parentId = Integer.valueOf(value);
                return new AbstractMap.SimpleEntry<>("parent", parentId);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Filter by parent id value should be an integer: " + ex.getLocalizedMessage(), ex);
            }
            return null;
        // just here to list the existing filter
        } else if ("profile".equals(f.getField()) || "level".equals(f.getField()) || "validation_required".equals(f.getField()) || "id".equals(f.getField())) {

            return new AbstractMap.SimpleEntry<>(f.getField(), value);

        } else {
            return new AbstractMap.SimpleEntry<>(f.getField(), value);
        }
    }

    /**
     * Returns a singleton map that contains the total count of matched records for filtered list.
     * and all records in a lightweight list of pojo {@link MetadataLightBrief}.
     * it is usefull to proceed to select All metadata to proceed to do batch actions on them.
     *
     * @param pagedSearch given {@link PagedSearch} that does not contains the pagination.
     * @param req the http request to extract the user infos
     * @param type type filter on metadata (DOC, MODELE, CONTACT,...)
     *
     * @return singleton Map of couple total count, list of lightweight record.
     */
    @RequestMapping(value="/metadatas/search/id",method=POST,produces=APPLICATION_JSON_VALUE)
    public Map searchIds(@RequestParam(name = "type", defaultValue = "DOC") final String type,
            @RequestBody final PagedSearch pagedSearch,
            final HttpServletRequest req) {

        final Map<String,Object> filterMap = prepareFilters(pagedSearch, req);
        filterMap.put("type", type);
        final List<MetadataLightBrief> list = metadataBusiness.filterAndGetWithoutPagination(filterMap);
        final Map<String,Object> result = new HashMap<>();
        result.put("total",list.size());
        result.put("list",list);
        return result;
    }

    /**
     * Return metadata brief object as json for given id.
     *
     * @param id given metadata id
     *
     * @return A {@link MetadataBrief}.
     */
    @RequestMapping(value="/metadatas/{id}",method=GET,produces=APPLICATION_JSON_VALUE)
    public MetadataBrief get(@PathVariable("id") final Integer id) {
        return metadataBusiness.getMetadataById(id);
    }

    /**
     * Return stats counts on metadata as a map.
     * This map contains :
     *  - total count of metadata.
     *  - total of metadata validated.
     *  - total of metadata not validated.
     *  - total of metadata waiting to validate.
     *  - total of metadata not published.
     *  - total of metadata published.
     *
     * @param type type filter on metadata (DOC, MODELE, CONTACT,...)
     *
     * @return A map of field / Integer.
     */
    @RequestMapping(value="/metadatas/stats",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getStats(@RequestParam(name = "type", defaultValue = "DOC") final String type) {
        final Map<String, Object> typeFilter = new HashMap<>();
        typeFilter.put("type", type);
        final Map<String,Integer> map = metadataBusiness.getStats(typeFilter);
        return new ResponseEntity(map, OK);
    }

    /**
     * Get all needed stats for given filters
     *
     * @param type type filter on metadata (DOC, MODELE, CONTACT,...)
     *
     * @param search pojo that contains filters.
     * @return A map with all metadata stats.
     */
    @RequestMapping(value="/metadatas/stats",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getFilteredStats(@RequestParam(name = "type", defaultValue = "DOC") final String type,
            @RequestBody final Search search) {

        final Map<String,Object> map = new HashMap<>();
        final List<Filter> filters = search.getFilters();
        final Map<String,Object> filterMap = new HashMap<>();
        filterMap.put("type", type);

        if(filters != null) {
            for(final Filter f : filters) {
                if("group".equals(f.getField())) {
                    String value = f.getValue();
                    if("_all".equals(value)) {
                        continue; //no need to filter on group field if we ask all groups
                    }
                    try{
                        final int groupId = Integer.valueOf(value);
                        filterMap.put("group",groupId);
                    }catch(Exception ex) {
                        //do nothing
                    }
                }else if ("period".equals(f.getField())) {
                    final String value = f.getValue();
                    if("_all".equals(value)) {
                        continue; //no need to filter on period if we ask from the beginning.
                    }
                    Long delta = Util.getDeltaTime(value);
                    if (delta == null) {
                        continue;
                    }
                    filterMap.put("period",delta);
                }
            }
        }

        final Map<String,Integer> general = metadataBusiness.getStats(filterMap);

        //Get profiles distribution counts
        final List<Profile> profiles = new ArrayList<>();
        final Map<String,Integer> profilesMap = metadataBusiness.getProfilesCount(filterMap);
        if(profilesMap!=null){
            for(final Map.Entry<String,Integer> entry : profilesMap.entrySet()){
                profiles.add(new Profile(entry.getKey(),entry.getValue()));
            }
        }
        map.put("repartitionProfiles",profiles);

        //Get completion counts for metadata in 10 categories (10%, 20%, ... 100%)
        final int[] completionArray = metadataBusiness.countInCompletionRange(filterMap);
        map.put("completionPercents",completionArray);

        final List<OwnerStatBrief> contributorsStatList = metadataBusiness.getOwnerStatBriefs(new HashMap<>(filterMap));
        map.put("contributorsStatList",contributorsStatList);

        final List<GroupStatBrief> groupsStatList = metadataBusiness.getGroupStatBriefs(new HashMap<>(filterMap));
        map.put("groupsStatList",groupsStatList);

        map.put("general",general);

        return new ResponseEntity(map, OK);
    }

    /**
     * Proceed to delete a list of metadata.
     * the http request provide the user that should be passed to check if the user can delete all record in the list.
     * the method must returns an appropriate response in case of user does not have the permission to delete all records.
     *
     * @param metadataList given metadata list to delete.
     * @return Response that contains all necessary info to inform the user about what records fails.
     */
    @RequestMapping(value="/metadatas/delete",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity delete(@RequestBody final List<MetadataBrief> metadataList) {

        //the user can select multiple records to delete,
        // but some of records can have a restricted permission for this user.
        //So we need to send an error message to prevent this case.
        final List<Integer> ids = new ArrayList<>();
        for (final MetadataBrief brief : metadataList) {
            ids.add(brief.getId());
        }
        try {
            metadataBusiness.deleteMetadata(ids);
            return new ResponseEntity("Records deleted with success.", OK);
        }catch(Throwable ex) {
            LOGGER.log(Level.WARNING,"Cannot delete metadata list due to exception error : "+ ex.getLocalizedMessage());
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Proceed to delete a metadata.
     *
     * @return Response that contains all necessary info to inform the user about what records fails.
     */
    @RequestMapping(value="/metadatas/{id}",method=DELETE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity delete(@PathVariable("id") final Integer id) {
        try {
            metadataBusiness.deleteMetadata(id);
            return new ResponseEntity("Record deleted with success.", OK);
        }catch(Throwable ex) {
            LOGGER.log(Level.WARNING,"Cannot delete metadata list due to exception error : "+ ex.getLocalizedMessage());
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Proceed to export metadata, creates all necessary files in tmp directory and returns file name
     * and directory name for next callback.
     * If there is one metadata into given list then it will creates xml file, otherwise
     * a zip file will be created.
     *
     * @param idList given id list
     * @return Response that contains the directory and file names.
     */
    @RequestMapping(value="/metadatas/export",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity exportMetadata(@RequestBody final List<Integer> idList) {

        Path directory = null;
        try {
            directory = Files.createTempDirectory(null);
            final List<Path> files = new ArrayList<>();
            for (final Integer id : idList) {
                try {
                    final MetadataBrief brief = metadataBusiness.getMetadataById(id);
                    final Path file = directory.resolve(cleanFileName(brief.getFileIdentifier()) + ".xml");
                    final Object metatadaObj = metadataBusiness.getMetadata(brief.getId());
                    IOUtilities.writeString(metadataBusiness.marshallMetadata(metatadaObj), file);
                    files.add(file);
                } catch (IOException | ConfigurationException ex) {
                    return new ErrorMessage(ex).build();
                }
            }

            final Path file;
            if (files.size() == 1) {
                file = files.get(0);
            } else {
                file = directory.resolve(UUID.randomUUID().toString() + ".zip");
                try {
                    ZipUtilities.zip(file, ZipOutputStream.DEFLATED, Deflater.BEST_COMPRESSION, null,
                            files.toArray(new Path[files.size()]));
                } catch (IOException ex) {
                    return new ErrorMessage(ex).build();
                }
            }

            Map<String, String> map = new HashMap<>();
            map.put("directory", directory.getFileName().toString());
            map.put("file", file.getFileName().toString());
            return new ResponseEntity(map, OK);

        } catch (IOException ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Utility function to clean the file name when exporting metadata to XML.
     * TODO use commons-utils instead.
     * @param s given string to clean
     * @return String value
     */
    private static String cleanFileName(String s) {
        s = s.replace(":", "_");
        s = s.replace("/", "_");
        return s;
    }

    /**
     * Download exported metadata for given file name and directory anme located in tmp folder.
     * this is the callback of exportMetadata method.
     *
     * @param directory given directory name.
     * @param file given file name to download
     *
     * @return Response as attachment file or error with status 500
     */
    @RequestMapping(value="/metadatas/download/{directory}/{file:.+}",method=GET)
    public ResponseEntity download(
            @PathVariable("directory") final String directory,
            @PathVariable("file") final String file, HttpServletResponse response) throws IOException {

        Path dir = null;
        try{
            dir = Paths.get(System.getProperty("java.io.tmpdir"), directory);
            final Path f = dir.resolve(file);

            response.addHeader("Content-Disposition","attachment; filename="+file);
            //response.setStatus(200);

            Files.copy(f,response.getOutputStream());
            response.flushBuffer();

            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING,"can't download MetaData : "+file,ex);
            return new ErrorMessage(ex).build();
        } finally {
            if(dir != null) {
                IOUtilities.deleteOnExit(dir);
            }
        }
    }

    /**
     * Change the owner id for given metadata list.
     *
     * @param ownerId given user id
     * @param metadataList the metadata list to apply changes
     *
     * @return HTTP code 200 if succeed.
     */
    @RequestMapping(value="/metadatas/owner/{ownerId}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity changeOwner(
            @PathVariable("ownerId") final int ownerId,
            @RequestBody final List<MetadataBrief> metadataList) {

        final List<Integer> ids = new ArrayList<>();
        for (final MetadataBrief brief : metadataList) {
            ids.add(brief.getId());
        }
        try {
            metadataBusiness.updateOwner(ids, ownerId);
            return new ResponseEntity("owner applied with success.",OK);
        }catch(Exception ex) {
            LOGGER.log(Level.WARNING,"Cannot change the owner for metadata list due to exception error : "+ ex.getLocalizedMessage());
            return new ErrorMessage(ex).status(FORBIDDEN).build();
        }
    }

    /**
     * Change the shared property for given metadata list.
     *
     * @param shared new shared value
     * @param metadataList the metadata list to apply changes
     * @return Response
     */
    @RequestMapping(value="/metadatas/shared/{shared}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity changeSharedProperty(
            @PathVariable("shared") final boolean shared,
            @RequestBody final List<MetadataBrief> metadataList) {

        final List<Integer> ids = new ArrayList<>();
        for (final MetadataBrief brief : metadataList) {
            ids.add(brief.getId());
        }
        try {
            metadataBusiness.updateSharedProperty(ids, shared);
            return new ResponseEntity("shared value applied with success.",OK);
        }catch(Exception ex) {
            LOGGER.log(Level.WARNING,"Cannot change the shared property for metadata list due to exception error : "+ ex.getLocalizedMessage());
            return new ErrorMessage(ex).status(FORBIDDEN).build();
        }
    }

    /**
     * Change the shared property for given metadata list.
     *
     * @param shared new shared value
     * @param id the metadata list to apply changes
     * @return Response
     */
    @RequestMapping(value="/metadatas/{id}/shared/{shared}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity changeSharedProperty(
            @PathVariable("shared") final boolean shared,
            @PathVariable("id") final int id) {
        try {
            metadataBusiness.updateSharedProperty(Arrays.asList(id), shared);
            return new ResponseEntity("shared value applied with success.",OK);
        }catch(Exception ex) {
            LOGGER.log(Level.WARNING,"Cannot change the shared property for metadata due to exception error : "+ ex.getLocalizedMessage());
            return new ErrorMessage(ex).status(FORBIDDEN).build();
        }
    }

    /**
     * Change the hidden property for given metadata list.
     *
     * @param hidden new hidden value
     * @param metadataList the metadata list to apply changes
     * @return Response
     */
    @RequestMapping(value="/metadatas/hidden/{hidden}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity changeHiddenProperty(
            @PathVariable("hidden") final boolean hidden,
            @RequestBody final List<MetadataBrief> metadataList) {

        final List<Integer> ids = new ArrayList<>();
        for (final MetadataBrief brief : metadataList) {
            ids.add(brief.getId());
        }
        try {
            metadataBusiness.updateHidden(ids, hidden);
            return new ResponseEntity("shared value applied with success.",OK);
        }catch(Exception ex) {
            LOGGER.log(Level.WARNING,"Cannot change the shared property for metadata list due to exception error : "+ ex.getLocalizedMessage());
            return new ErrorMessage(ex).status(FORBIDDEN).build();
        }
    }

    /**
     * Change the hidden property for given metadata list.
     *
     * @param hidden new hidden value
     * @param id the metadata list to apply changes
     * @return Response
     */
    @RequestMapping(value="/metadatas/{id}/hidden/{hidden}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity changeHiddenProperty(
            @PathVariable("hidden") final boolean hidden,
            @PathVariable("id") final int id) {
        try {
            metadataBusiness.updateHidden(Arrays.asList(id), hidden);
            return new ResponseEntity("shared value applied with success.",OK);
        }catch(Exception ex) {
            LOGGER.log(Level.WARNING,"Cannot change the shared property for metadata due to exception error : "+ ex.getLocalizedMessage());
            return new ErrorMessage(ex).status(FORBIDDEN).build();
        }
    }

    /**
     * Change the validation state for given metadata list.
     * @param isvalid given state of validation
     * @param validationList the metadata identifier list to apply changes
     *                       with optional comment in case of discarding validation.
     *
     * @return A map descibing the status of the operation and eventually the error reason
     */
    @RequestMapping(value="/metadatas/validation/{isvalid}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity changeValidation(
            @PathVariable("isvalid") final boolean isvalid,
            @RequestBody final ValidationList validationList,
            final HttpServletRequest req) {

        final List<Integer> metadataList = validationList.getMetadataList();
        final String comment = validationList.getComment();

        boolean canContinue = true;
        final List<MetadataBrief> list = new ArrayList<>();
        for (final Integer  id : metadataList) {
            final MetadataBrief metadata = metadataBusiness.getMetadataById(id);
            if(metadata == null) {
                //skip if null, never happen
                continue;
            }
            list.add(metadata);
            if(isvalid && "NONE".equalsIgnoreCase(metadata.getLevelCompletion()) && isLevelRequiredForValidation()) {
                canContinue = false;
                break; //no needs to continue in the loop because there are metadata with level=NONE.
            }
        }
        final Map<String,String> map = new HashMap<>();
        if(canContinue) {
            try {
                final String url = req.getRequestURL().toString();
                final String baseUrl = url.substring(0,url.indexOf("API/metadatas"));
                final String tmplMDLink = baseUrl+"admin.html#/metadata?tab=metadata&id=";
                for (final MetadataBrief md : list) {
                    if(isvalid) {
                        if("REQUIRED".equalsIgnoreCase(md.getValidationRequired())){
                            metadataBusiness.acceptValidation(md,tmplMDLink);
                        }else {
                            metadataBusiness.updateValidation(md.getId(),true);
                        }
                    } else if("REQUIRED".equalsIgnoreCase(md.getValidationRequired())){
                        metadataBusiness.denyValidation(md,comment,tmplMDLink);
                    } else {
                        if(md.getIsPublished()) {
                            metadataBusiness.updatePublication(md.getId(),false);
                        }
                        metadataBusiness.updateValidation(md.getId(),false);
                    }
                }
                map.put("status","ok");
                return new ResponseEntity(map, OK);
            }catch(Exception ex) {
                LOGGER.log(Level.WARNING,"Cannot change the validation for metadata list due to exception error : "+ ex.getLocalizedMessage());
                return new ErrorMessage(ex).status(FORBIDDEN).build();
            }
        }
        map.put("notLevel","true");
        map.put("status","failed");
        return new ResponseEntity(map, FORBIDDEN);
    }

    /**
     * Change the published state for given metadata list.
     *
     * @param ispublished given state of published flag.
     * @param metadataList the metadata list to apply changes
     *
     * @return A map descibing the status of the operation and eventually the error reason
     */
    @RequestMapping(value="/metadatas/publication/{ispublished}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity changePublication(
            @PathVariable("ispublished") final boolean ispublished,
            @RequestBody final List<MetadataBrief> metadataList) {

        boolean canContinue = true;

        for (final MetadataBrief brief : metadataList) {
            final int id = brief.getId();
            final MetadataBrief metadata = metadataBusiness.getMetadataById(id);
            if(metadata == null) {
                //skip if null, never happen
                continue;
            }
            if(ispublished && !metadata.getIsValidated()) {
                canContinue = false;
                break; //no needs to continue in the loop because there are not valid metadata.
            }
        }
        final Map<String,String> map = new HashMap<>();
        if(canContinue) {
            final List<Integer> ids = new ArrayList<>();
            for (final MetadataBrief brief : metadataList) {
                ids.add(brief.getId());
            }
            try {
                metadataBusiness.updatePublication(ids, ispublished);
                map.put("status","ok");
                return new ResponseEntity(map, OK);
            }catch(Exception ex) {
                LOGGER.log(Level.WARNING,"Cannot change the publication state for metadata list due to exception error : "+ ex.getLocalizedMessage());
                return new ErrorMessage(ex).status(FORBIDDEN).build();
            }
        }

        map.put("notValidExists","true");
        map.put("status","failed");
        return new ResponseEntity(map, FORBIDDEN);
    }

    /**
     * Ask to an administrator the validation of a metadata list.
     *
     * @param metadataList A List of metadata brief to validate.
     * @param req
     *
     * @return A map descibing the status of the operation and eventually the error reason
     */
    @RequestMapping(value="/metadatas/askForValidation",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity askForValidation(@RequestBody final List<MetadataBrief> metadataList,
            HttpServletRequest req) {
        try {
            final Integer userId = assertAuthentificated(req);
            boolean canContinue = true;
            boolean validExists = false;
            boolean notOwner = false;
            for (final MetadataBrief brief : metadataList) {
                final int id = brief.getId();

                //we need to get the ownerId of metadata, the given brief.getUser can be null
                // especially in case of when using selectAll to call this action by batch.
                //So we get the owner from the metadata pojo to prevent this case
                // and make sure we have the owner of metadata.

                final MetadataBrief metadata = metadataBusiness.getMetadataById(id);
                if(metadata == null) {
                    //skip if null, never happen
                    continue;
                }
                if(!metadata.getIsValidated()) {
                    if(!userId.equals(metadata.getOwner())) {
                        notOwner = true;
                        canContinue = false;
                    }
                }else {
                    validExists = true;
                    canContinue = false;
                }
            }
            final Map<String,String> map = new HashMap<>();
            if(canContinue) {

                    final List<Integer> ids = new ArrayList<>();
                    for (final MetadataBrief brief : metadataList) {
                        ids.add(brief.getId());
                    }
                    final String url = req.getRequestURL().toString();
                    final String baseUrl = url.substring(0,url.indexOf("API/metadata"));
                    final String tmplMDLink = baseUrl+"admin.html#/metadata?tab=metadata&id=";
                    metadataBusiness.askForValidation(ids,tmplMDLink,true);
                    map.put("status","ok");
                    return new ResponseEntity(map, OK);

            }

            map.put("notOwner",""+notOwner);
            map.put("validExists",""+validExists);
            map.put("status","failed");
            return new ResponseEntity(map, FORBIDDEN);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,"Cannot proceed to ask validation for metadata list due to exception error : "+ ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).status(FORBIDDEN).build();
        }
    }

    /**
     * Returns the json representation of metadata by using template for given metadata ID .
     * the metadata can be pruned in case of displaying purposes, or set prune to false for edition purposes.
     *
     * @param id given metadata ID
     * @param prune flag that indicates if the metadata will be pruned or not to delete empty values.
     * @return ResponseEntity that contains the metadata in json format.
     */
    @RequestMapping(value="/metadatas/{id}/json",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getIsoMetadataJson(@PathVariable("id") final int id,
            @RequestParam("prune") final boolean prune, HttpServletResponse response) {
        try{
            final StringWriter buffer = new StringWriter();
            Object metadata = metadataBusiness.getMetadata(id);
            if (metadata != null) {
                if(prune && metadata instanceof DefaultMetadata){
                    ((DefaultMetadata)metadata).prune();
                }
                //get template name
                final String templateName = metadataBusiness.getMetadataById(id).getType();
                final Template template = templateResolver.getByName(templateName);
                template.write(metadata,buffer,prune, false);
            }
            IOUtils.write(buffer.toString(), response.getOutputStream());
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, "error while writing metadata json.", ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Returns the json representation by using a template for a new metadata with default values.
     *
     * @param profile the given profile name
     * @return ResponseEntity that contains the metadata in json format.
     */
    @RequestMapping(value="/metadatas/json/new",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getNewMetadataJson(@RequestParam(name = "profile", required = true) final String profile,
            HttpServletResponse response) {
        try {
            final StringWriter buffer = new StringWriter();
            //get template name
            final Template template = templateResolver.getByName(profile);
            template.write(null, buffer, false, false);
            IOUtils.write(buffer.toString(), response.getOutputStream());
            return new ResponseEntity(OK);

        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, "An error happen when building json representation of new metadata for profile "+profile, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Convert metadata in given profile and return the json representation of the resulted metadata.
     * This method do not perform a save on the metadata.
     *
     * @param id given metadata id
     * @param prune optional flag that indicates if the prune will be applied
     * @param profile the target  profile name
     *
     * @return The json representation of the converted metadata.
     */
    @RequestMapping(value="/metadatas/{id}/json/convert",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity convertMetadataJson(
            @PathVariable("id") final int id,
            @RequestParam("prune")      final boolean prune,
            @RequestParam("profile")    final String profile,
            HttpServletResponse response) {

        try {
            final StringWriter buffer = new StringWriter();
            Object metadata = metadataBusiness.getMetadata(id);
            if (metadata != null) {
                if (prune && metadata instanceof DefaultMetadata){
                    ((DefaultMetadata)metadata).prune();
                }
                Template newTemplate = templateResolver.getByName(profile);
                newTemplate.write(metadata, buffer, false, true);
            }
            IOUtils.write(buffer.toString(), response.getOutputStream());
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, "error while writing metadata json.", ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Proceed to save metadata values for given metadata id and values
     * using template defined by given profile.
     *
     * @param id given metadata id
     * @param profile given profile, can be another profile of metadata's own
     * @param metadataValues {@code RootObj} metadata values to save
     * @return {code Response}
     */
    @RequestMapping(value="/metadatas/{id}/save",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity saveMetadata(
            @PathVariable("id") final int id,
            @RequestParam("profile") final String profile,
            @RequestBody final RootObj metadataValues) {

        try {
            // Get previously saved metadata
            final MetadataBrief pojo = metadataBusiness.getMetadataPojo(id);
            if (pojo != null) {

                //get template
                final Template template = templateResolver.getByName(profile);

                // detect profile change
                final Object metadata;
                if (!pojo.getType().equals(profile)) {
                    metadata = template.emptyMetadata();
                    metadataBusiness.updateProfile(id, profile);
                } else {
                    metadata = metadataBusiness.getMetadata(id);
                }

                template.read(metadataValues, metadata, false);

                //update dateStamp for metadata
                if(metadata instanceof DefaultMetadata){
                    ((DefaultMetadata)metadata).setDateStamp(new Date());
                }

                //Save metadata
                final String metadataID = template.getMetadataIdentifier(metadata);
                metadataBusiness.updateMetadata(metadataID, metadata, null, null, null, null, pojo.getProviderId(), pojo.getDocType(), null, pojo.getIsHidden());
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "error while saving metadata.", ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity("Metadata saved successfully.", OK);
    }

    /**
     * Proceed to create new metadata for given profile and values.
     *
     * @param profile given profile
     * @param type The metadata type (DOC, MODELE, CONTACT, ...)
     * @param metadataValues {@code RootObj} metadata values to save
     *
     * @return The created metadata brief.
     */
    @RequestMapping(value="/metadatas",method = POST,consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity create(
            @RequestParam(name = "profile", required = true, defaultValue = "profile_import") final String profile,
            @RequestParam(name = "type",    required = true, defaultValue = "DOC") final String type,
            @RequestBody final RootObj metadataValues) {
        final MetadataBrief brief;
        try {
            //get template
            final Template template = templateResolver.getByName(profile);
            final Object metadata = template.emptyMetadata();
            template.read(metadataValues, metadata, true);
            String identifier = template.getMetadataIdentifier(metadata);
            if (identifier == null) {
                identifier = UUID.randomUUID().toString();
                template.setMetadataIdentifier(identifier, metadata);
            }
            if (metadata instanceof DefaultMetadata) {
                ((DefaultMetadata)metadata).setDateStamp(new Date());
            }
            final Integer internProviderID = metadataBusiness.getDefaultInternalProviderID();
            if (internProviderID != null) {
                final MetadataLightBrief md = metadataBusiness.updateMetadata(identifier, metadata, null, null, null, null, internProviderID, type);
                metadataBusiness.updateProfile(md.getId(), profile);
                brief = metadataBusiness.getMetadataById(md.getId());
            } else {
                throw new ConfigurationException("No default internal metadata provider available.");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "An error happen when creating new metadata.", ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(brief, OK);
    }

    /**
     * Proceed to duplicate metadata for given id.
     * an optional title is given to set the new title of cloned metadata.
     * if the given title is empty or null
     *
     * @param id given metadata id to duplicate
     * @param title Optional title
     * @param type Optional type
     * @return Response
     */
    @RequestMapping(value="/metadatas/{id}/duplicate",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity duplicateMetadata(@PathVariable("id") final int id,
            @RequestParam(name = "title", required = false) final String title,
            @RequestParam(name = "type", required = false) final String type) {

        try {
            final String newTitle;
            if(!StringUtils.isBlank(title)){
                newTitle = title;
            }else {
                newTitle = null;
            }
            metadataBusiness.duplicateMetadata(id, newTitle, type);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity("Metadata duplicated successfully.", OK);
    }

    /**
     * Create a link between an attachment and a metadata.
     *
     * @param id Metadata identifier.
     * @param attId Attachment identifier.
     * @return
     */
    @RequestMapping(value="/metadatas/{id}/attachment/{attId}",method=POST)
    public ResponseEntity linkAttachment(
            @PathVariable("id") final int id,
            @PathVariable("attId") final int attId) {

        try {
            metadataBusiness.linkMetadataAtachment(id, attId);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Remove a link between an attachment and a metadata.
     *
     * @param id Metadata identifier.
     * @param attId Attachment identifier.
     *
     * @return HTTP code 200 if succeed.
     */
    @RequestMapping(value="/metadatas/{id}/attachment/{attId}",method=DELETE)
    public ResponseEntity unlinkAttachment(
            @PathVariable("id") final int id,
            @PathVariable("attId") final int attId) {

        try {
            metadataBusiness.unlinkMetadataAtachment(id, attId);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Receive a {@code MultiPart} which contains xml file,
     * the metadata will be stored in server and returns the generated Id
     * and the nearest profile name that matches the metadata.
     *
     * @param mdFileIs {@code InputStream} the given xml stream
     * @param type The metadata type (DOC, MODELE, CONTACT, ...)
     * @param profile FOrce the profile for the specified value. (can be {@code null}).
     *
     * @param request {@code HttpServletRequest} the hhtp request
     * @return {@code Response} with 200 code if upload work, 500 if not work.
     */
    @RequestMapping(value="/metadatas/upload",method=POST,consumes=MULTIPART_FORM_DATA_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity uploadMetadata(
            @RequestParam("metadata") MultipartFile mdFileIs,
            @RequestParam(name = "type", required = true, defaultValue = "DOC") final String type,
            @RequestParam(name = "profile", required = false) final String profile,
            final HttpServletRequest request) {

        final Map<String,Object> map = new HashMap<>();
        try {
            if (mdFileIs != null) {
                final Path uploadDirectory = getUploadDirectory();
                final Path newFileMetaData = uploadDirectory.resolve(mdFileIs.getOriginalFilename());

                try (InputStream in = mdFileIs.getInputStream()) {
                    Files.copy(in, newFileMetaData, StandardCopyOption.REPLACE_EXISTING);
                }

                Integer internalProviderID = metadataBusiness.getDefaultInternalProviderID();
                if (internalProviderID != null) {
                    final Object iso = metadataBusiness.unmarshallMetadata(newFileMetaData);

                    String identifier = Utils.findIdentifier(iso);
                    if (metadataBusiness.existInternalMetadata(identifier, true, false, internalProviderID)) {
                        identifier = UUID.randomUUID().toString();
                        Utils.setIdentifier(identifier, iso);
                        map.put("renewId", true);
                    } else {
                        map.put("renewId", false);
                    }
                    Boolean usedDefaultProfile = templateResolver.resolveDefaultFromMetadata(iso) == null;
                    map.put("usedDefaultProfile", usedDefaultProfile);
                    final MetadataLightBrief meta = metadataBusiness.updateMetadata(identifier, iso, null, null, null, null, internalProviderID, type, profile, false);

                    MetadataBrief brief = metadataBusiness.getMetadataById(meta.getId());
                    map.put("record", brief);
                } else {
                    throw new ConfigurationException("No default internal metadata provider available.");
                }
            }
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(map, OK);
    }

    /**
     * Used to upload generic data as an attachment.
     * Returns a JSON response with attachment ID.
     *
     * @param file
     * @return
     */
    @RequestMapping(value="/attachments/upload",method=POST,consumes=MULTIPART_FORM_DATA_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity uploadAttachment(@RequestParam("data") MultipartFile file) {
        final int attId;
        try (InputStream in = file.getInputStream()) {
            attId = metadataBusiness.createMetadataAttachment(in, file.getOriginalFilename());
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(attId, OK);
    }

    @RequestMapping(value = "/attachments/view/{attachmentId}", method = RequestMethod.GET)
    public ResponseEntity get(@PathVariable("attachmentId") Integer attachmentId, final HttpServletResponse response) {
        if(attachmentId != null) {
            final Attachment mql   = metadataBusiness.getMetadataAttachment(attachmentId);
            final byte[] quicklook = mql.getContent();
            final String fileName  = mql.getFilename();

            response.setCharacterEncoding("UTF-8");
            final HttpHeaders responseHeaders = new HttpHeaders();

            // backward compatibility when there was no file name and the resource has been converted to PNG
            if (fileName == null) {
                response.setContentType(MediaType.IMAGE_PNG_VALUE);
                responseHeaders.set("Content-Type", "image/png");
            } else {
                try {
                    //try to get the content type of the attached file by file name
                    String mimeType = MIME_TYPE_MAP.getContentType(fileName);

                    final String mediaType = MediaType.valueOf(mimeType).toString();
                    response.setContentType(mediaType);
                    responseHeaders.set("Content-Type", mediaType);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                    response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                    responseHeaders.set("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
                }
            }
            try {
                IOUtils.write(quicklook, response.getOutputStream());
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error while writing attached file response", ex);
            }
            return new ResponseEntity(responseHeaders,OK);
        }
        return null;
    }

    /**
     * Return a map of associted data for each metadata id specified
     * @param params
     * @return
     */
    @RequestMapping(value="/metadatas/data",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getAssociatedData(@RequestBody final List<String> params) {

        final Map<String, List<DataBrief>> mapping = new HashMap<>();
        for (final String mdId : params) {
            if (mdId != null) {
                final List<DataBrief> dataBriefs = dataBusiness.getDataBriefsFromMetadataId(mdId);
                mapping.put(mdId, dataBriefs);
            }
        }
        return new ResponseEntity(mapping, OK);
    }

    /**
     * Allow to deactivate the requirement of level completion for metadata validation.
     * Used for development purpose.
     * @return boolean value
     */
    private boolean isLevelRequiredForValidation() {
        String value = configurationBusiness.getProperty("validation.require.level");
        return value == null || Boolean.parseBoolean(value);
    }

    /**
     * Create a link between an Data and a metadata.
     *
     * @param id Metadata identifier.
     * @param dataId Data identifier.
     * @return
     */
    @RequestMapping(value="/metadatas/{id}/data/{dataId}",method=POST)
    public ResponseEntity linkData(
            @PathVariable("id") final int id,
            @PathVariable("dataId") final int dataId) {

        try {
            metadataBusiness.linkMetadataData(id, dataId);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Remove a link between an attachment and a metadata.
     *
     * @param id Metadata identifier.
     * @param dataId Data identifier.
     * @return
     */
    @RequestMapping(value="/metadatas/{id}/data/{dataId}",method=DELETE)
    public ResponseEntity unlinkData(
            @PathVariable("id") final int id,
            @PathVariable("dataId") final int dataId) {

        try {
            metadataBusiness.unlinkMetadataData(id, dataId);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Create a link between an Data and a metadata.
     *
     * @param id Metadata identifier.
     * @param datasetId Dataset identifier.
     * @return
     */
    @RequestMapping(value="/metadatas/{id}/dataset/{datasetId}",method=POST)
    public ResponseEntity linkDataset(
            @PathVariable("id") final int id,
            @PathVariable("datasetId") final int datasetId) {

        try {
            metadataBusiness.linkMetadataDataset(id, datasetId);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Remove a link between an attachment and a metadata.
     *
     * @param id Metadata identifier.
     * @param datasetId Dataset identifier.
     * @return
     */
    @RequestMapping(value="/metadatas/{id}/dataset/{datasetId}",method=DELETE)
    public ResponseEntity unlinkDataset(
            @PathVariable("id") final int id,
            @PathVariable("datasetId") final int datasetId) {

        try {
            metadataBusiness.unlinkMetadataDataset(id, datasetId);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Create a link between an Data and a metadata.
     *
     * @param id Metadata identifier.
     * @param mapcontextId Mapcontext identifier.
     * @return
     */
    @RequestMapping(value="/metadatas/{id}/mapcontext/{mapcontextId}",method=POST)
    public ResponseEntity linkMapcontext(
            @PathVariable("id") final int id,
            @PathVariable("mapcontextId") final int mapcontextId) {

        try {
            metadataBusiness.linkMetadataMapContext(id, mapcontextId);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Remove a link between an attachment and a metadata.
     *
     * @param id Metadata identifier.
     * @param mapcontextId Mapcontext identifier.
     * @return
     */
    @RequestMapping(value="/metadatas/{id}/mapcontext/{mapcontextId}",method=DELETE)
    public ResponseEntity unlinkMapcontext(
            @PathVariable("id") final int id,
            @PathVariable("mapcontextId") final int mapcontextId) {

        try {
            metadataBusiness.unlinkMetadataMapContext(id, mapcontextId);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

}
