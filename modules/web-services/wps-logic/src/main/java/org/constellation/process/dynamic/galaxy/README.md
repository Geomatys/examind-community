# Galaxy Configuration

In the configuration file :
```
modules/library/cstl-lib-api/src/main/resources/org/constellation/configuration/constellation.properties
```
Set the galaxy variables :
- **examind.galaxy.url** with your galaxy service URL
- **examind.galaxy.access.key** with the access / api key

*Note : API key can be found here in User > Preferences > Manage API Key*

*For earth-system galaxy : https://earth-system.usegalaxy.eu/user/api_key*

```properties
# Example
examind.galaxy.url=https://earth-system.usegalaxy.eu
examind.galaxy.access.key=<access-key>
```

---

Here is an example of xml for a galaxy process invocation : 
```xml
<ns31:ProcessOfferings xmlns:ns34="http://www.metalinker.org/" xmlns:ns33="http://www.opengis.net/wps/1.0.0" xmlns:ns32="http://www.w3.org/1998/Math/MathML" xmlns:ns31="http://www.opengis.net/wps/2.0" xmlns:ns30="http://www.opengis.net/ows/1.1" xmlns:ns29="http://www.opengis.net/ows/2.0" xmlns:mdb="http://standards.iso.org/iso/19115/-3/mdb/1.0" xmlns:gcx="http://standards.iso.org/iso/19115/-3/gcx/1.0" xmlns:ns26="http://www.opengis.net/gml" xmlns:msr="http://standards.iso.org/iso/19115/-3/msr/1.0" xmlns:lan="http://standards.iso.org/iso/19115/-3/lan/1.0" xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:mrc="http://standards.iso.org/iso/19115/-3/mrc/1.0" xmlns:gmd="http://www.isotc211.org/2005/gmd" xmlns:mpc="http://standards.iso.org/iso/19115/-3/mpc/1.0" xmlns:srv1="http://www.isotc211.org/2005/srv" xmlns:mrs="http://standards.iso.org/iso/19115/-3/mrs/1.0" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mco="http://standards.iso.org/iso/19115/-3/mco/1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0" xmlns:dqm="http://standards.iso.org/iso/19157/-2/dqm/1.0" xmlns:mrd="http://standards.iso.org/iso/19115/-3/mrd/1.0" xmlns:mri="http://standards.iso.org/iso/19115/-3/mri/1.0" xmlns:mdq="http://standards.iso.org/iso/19157/-2/mdq/1.0" xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0" xmlns:mas="http://standards.iso.org/iso/19115/-3/mas/1.0" xmlns:mac="http://standards.iso.org/iso/19115/-3/mac/1.0" xmlns:mmi="http://standards.iso.org/iso/19115/-3/mmi/1.0" xmlns:gex="http://standards.iso.org/iso/19115/-3/gex/1.0" xmlns:srv="http://standards.iso.org/iso/19115/-3/srv/2.0" xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0" xmlns:mex="http://standards.iso.org/iso/19115/-3/mex/1.0">
    <ns31:ProcessOffering jobControlOptions="sync-execute async-execute dismiss" outputTransmission="reference value" processModel="native" processVersion="1.0.0">
        <ns31:Process>
            <ns29:Title xml:lang="en-EN">Marine Omics visualisation</ns29:Title>
            <ns29:Abstract xml:lang="en-EN">From Obis data to Biodiversity indicators</ns29:Abstract>
            <ns29:Identifier>urn:exa:wps:examind::marine:omics</ns29:Identifier>

            <ns31:Input>
                <ns29:Title xml:lang="en-EN">New History Name</ns29:Title>
                <ns29:Abstract xml:lang="en-EN">new history name</ns29:Abstract>
                <ns29:Identifier>urn:exa:wps:examind::marine:omics:input:new_history_name</ns29:Identifier>
                <ns31:LiteralData>
                    <ns31:Format mimeType="text/plain" default="true"/>
                    <LiteralDataDomain>
                        <ns29:AnyValue/>
                        <ns29:DataType ns29:reference="http://www.w3.org/TR/xmlschema-2/#string">String</ns29:DataType>
                    </LiteralDataDomain>
                </ns31:LiteralData>
            </ns31:Input>
            <ns31:Input>
                <ns29:Title xml:lang="en-EN">Use Cached Job</ns29:Title>
                <ns29:Abstract xml:lang="en-EN">use cached job</ns29:Abstract>
                <ns29:Identifier>urn:exa:wps:examind::marine:omics:input:use_cached_job</ns29:Identifier>
                <ns31:LiteralData>
                    <ns31:Format mimeType="text/plain" default="true"/>
                    <LiteralDataDomain>
                        <ns29:AnyValue/>
                        <ns29:DataType ns29:reference="http://www.w3.org/TR/xmlschema-2/#boolean">Boolean</ns29:DataType>
                    </LiteralDataDomain>
                </ns31:LiteralData>
            </ns31:Input>
            <ns31:Input minOccurs="1" maxOccurs="1">
                <ns29:Title xml:lang="en-EN">Inputs</ns29:Title>
                <ns29:Abstract xml:lang="en-EN">inputs</ns29:Abstract>
                <ns29:Identifier>urn:exa:wps:examind::marine:omics:input:inputs</ns29:Identifier>
                <ns31:ComplexData>
                    <ns31:Format mimeType="application/json" default="true"/>
                </ns31:ComplexData>
            </ns31:Input>
            <ns31:Input minOccurs="1" maxOccurs="1">
                <ns29:Title xml:lang="en-EN">Parameters</ns29:Title>
                <ns29:Abstract xml:lang="en-EN">parameters</ns29:Abstract>
                <ns29:Identifier>urn:exa:wps:examind::marine:omics:input:parameters</ns29:Identifier>
                <ns31:ComplexData>
                    <ns31:Format mimeType="application/json" default="true"/>
                    <![CDATA[{
                        "0":{
                            "long_min":"6.8",
                            "long_max":"9.0"
                            },
                        "1":{
                            "complement":"",
                            "delimiter":"",
                            "cut_type_options|cut_element":"-f",
                            "cut_type_options|list":["1","7","8","44","78"]
                            }
                        }
                    ]]>
                </ns31:ComplexData>
            </ns31:Input>
            <ns31:Input>
                <ns29:Title xml:lang="en-EN">Parameters Normalized</ns29:Title>
                <ns29:Abstract xml:lang="en-EN">parameters_normalized</ns29:Abstract>
                <ns29:Identifier>urn:exa:wps:examind::marine:omics:input:parameters_normalized</ns29:Identifier>
                <ns31:LiteralData>
                    <ns31:Format mimeType="text/plain" default="true"/>
                    <LiteralDataDomain>
                        <ns29:AnyValue/>
                        <ns29:DataType ns29:reference="http://www.w3.org/TR/xmlschema-2/#boolean">Boolean</ns29:DataType>
                    </LiteralDataDomain>
                </ns31:LiteralData>
            </ns31:Input>
            <ns31:Input>
                <ns29:Title xml:lang="en-EN">Batch</ns29:Title>
                <ns29:Abstract xml:lang="en-EN">batch</ns29:Abstract>
                <ns29:Identifier>urn:exa:wps:examind::marine:omics:input:batch</ns29:Identifier>
                <ns31:LiteralData>
                    <ns31:Format mimeType="text/plain" default="true"/>
                    <LiteralDataDomain>
                        <ns29:AnyValue/>
                        <ns29:DataType ns29:reference="http://www.w3.org/TR/xmlschema-2/#boolean">Boolean</ns29:DataType>
                    </LiteralDataDomain>
                </ns31:LiteralData>
            </ns31:Input>

            <ns31:Output>
                <ns29:Title xml:lang="en-EN">Dataset Collections Result</ns29:Title>
                <ns29:Abstract xml:lang="en-EN">dataset collections result</ns29:Abstract>
                <ns29:Identifier>urn:exa:wps:examind::marine:omics:output:dataset-collections</ns29:Identifier>
                <ns31:LiteralData>
                    <ns31:Format mimeType="text/plain" default="true"/>
                    <LiteralDataDomain>
                        <ns29:AnyValue/>
                        <ns29:DataType ns29:reference="http://www.w3.org/TR/xmlschema-2/#string">String</ns29:DataType>
                    </LiteralDataDomain>
                </ns31:LiteralData>
            </ns31:Output>
            <ns31:Output>
                <ns29:Title xml:lang="en-EN">Dataset Result</ns29:Title>
                <ns29:Abstract xml:lang="en-EN">dataset result</ns29:Abstract>
                <ns29:Identifier>urn:exa:wps:examind::marine:omics:output:dataset</ns29:Identifier>
                <ns31:LiteralData>
                    <ns31:Format mimeType="text/plain" default="true"/>
                    <LiteralDataDomain>
                        <ns29:AnyValue/>
                        <ns29:DataType ns29:reference="http://www.w3.org/TR/xmlschema-2/#string">String</ns29:DataType>
                    </LiteralDataDomain>
                </ns31:LiteralData>
            </ns31:Output>

        </ns31:Process>
    </ns31:ProcessOffering>
</ns31:ProcessOfferings>
```