INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('depth',                  'Depth',                             'urn:ogc:def:phenomenon:GEOM:depth',                 'the depth in water');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('temperature',            'Temperature',                       'urn:ogc:def:phenomenon:GEOM:temperature',           'the temperature in celcius degree');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('aggregatePhenomenon',    'Aggregate Phenomenon',              'urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon',   'the aggregation of temperature and depth phenomenons');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('salinity',               'Salinity',                          'urn:ogc:def:phenomenon:GEOM:salinity',              'the salinity in water');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('aggregatePhenomenon-2',  'Aggregate Phenomenon 2',            'urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon-2', 'the aggregation of temperature depth, and salinity phenomenons');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('isHot',                  'Hotness',                           'urn:ogc:def:phenomenon:GEOM:isHot',                 'hotness indicator');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('color',                  'Color',                             'urn:ogc:def:phenomenon:GEOM:color',                 'the color label');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('expiration',             'Expiration Date',                   'urn:ogc:def:phenomenon:GEOM:expiration',            'Expiration date');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('age',                    'Age',                               'urn:ogc:def:phenomenon:GEOM:age',                   'current age');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('metadata',              'Metadata',                           'urn:ogc:def:phenomenon:GEOM:metadata',              'some metadata');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('multi-type-phenomenon',  'Multi type phenomenon',             'urn:ogc:def:phenomenon:GEOM:multi-type-phenomenon', 'the aggregation of variable phenomenons type');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('multi-type-phenprofile', 'Multi type phenomenon for Profile', 'urn:ogc:def:phenomenon:GEOM:multi-type-phenprofile', 'the aggregation of variable phenomenons type for profile');

INSERT INTO "om"."observed_properties_properties" ("id_phenomenon", "property_name", "value") VALUES ('aggregatePhenomenon',   'phen-category',  'physics');
INSERT INTO "om"."observed_properties_properties" ("id_phenomenon", "property_name", "value") VALUES ('aggregatePhenomenon-2', 'phen-category',  'elementary');
INSERT INTO "om"."observed_properties_properties" ("id_phenomenon", "property_name", "value") VALUES ('depth',                 'phen-category',  'biological');
INSERT INTO "om"."observed_properties_properties" ("id_phenomenon", "property_name", "value") VALUES ('depth',                 'phen-category',  'organics');
INSERT INTO "om"."observed_properties_properties" ("id_phenomenon", "property_name", "value") VALUES ('depth',                 'phen-usage',     'production');
INSERT INTO "om"."observed_properties_properties" ("id_phenomenon", "property_name", "value") VALUES ('temperature',           'phen-category',  'biological');
INSERT INTO "om"."observed_properties_properties" ("id_phenomenon", "property_name", "value") VALUES ('aggregatePhenomenon',   'phen-usage',     'studies');

INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('aggregatePhenomenon', 'depth', 0);
INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('aggregatePhenomenon', 'temperature', 1);

INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('aggregatePhenomenon-2', 'depth', 0);
INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('aggregatePhenomenon-2', 'temperature', 1);
INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('aggregatePhenomenon-2', 'salinity', 2);

INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('multi-type-phenomenon', 'isHot',      0);
INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('multi-type-phenomenon', 'color',      1);
INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('multi-type-phenomenon', 'expiration', 2);
INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('multi-type-phenomenon', 'age',        3);

INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('multi-type-phenprofile', 'depth',      0);
INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('multi-type-phenprofile', 'isHot',      1);
INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('multi-type-phenprofile', 'color',      2);
INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('multi-type-phenprofile', 'expiration', 3);
INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('multi-type-phenprofile', 'age',        4);
INSERT INTO "om"."components" ("phenomenon", "component", "order") VALUES ('multi-type-phenprofile', 'metadata',   5);
---------

INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:1',              x'000000000140efef0000000000413a6b2800000000', 27582, 1,  NULL, 'system',    'timeseries', 'Sensor 1',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:2',              x'000000000140f207a9e96900384139bf0a15544d08', 27582, 2,  NULL, 'component', 'profile',    'Sensor 2',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:3',              x'00000000014044000000000000c008000000000000', 4326,  3,  NULL, 'system',    'timeseries', 'Sensor 3',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:4',              x'000000000140240000000000004024000000000000', 4326,  4,  NULL, 'system',    'timeseries', 'Sensor 4',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:test-1',         x'000000000140140000000000004024000000000000', 4326,  5,  NULL, 'system',    'timeseries', 'Sensor test 1',     null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:6',              x'000000000140140000000000004014000000000000', 4326,  6,  NULL, 'system',    'timeseries', 'Sensor 6',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:7',              x'00000000014145b7ca31487fc1c138da59f139abf8', 27582, 7,  NULL, 'system',    'timeseries', 'Sensor 7',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:8',              x'000000000140efef0000000000413a6b2800000000', 27582, 8,  NULL, 'system',    'timeseries', 'Sensor 8',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:9',              x'000000000140efef0000000000413a6b2800000000', 27582, 9,  NULL, 'system',    'simple',     'Sensor 9',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:10',             x'000000000140efef0000000000413a6b2800000000', 27582, 10, NULL, 'system',    'timeseries', 'Sensor 10',         null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:test-id',        x'000000000140efef0000000000413a6b2800000000', 27582, 11, NULL, 'system',    'timeseries', 'Sensor test id',    null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:12',             x'000000000140efef0000000000413a6b2800000000', 27582, 12, NULL, 'system',    'timeseries', 'Sensor 12',         null, 2);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:13',             x'00000000014044000000000000c008000000000000',  4326, 13, NULL, 'system',    'timeseries', 'Sensor 13',         null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:14',             x'000000000140f207a9e96900384139bf0a15544d08', 27582, 14, NULL, 'system',    'profile',    'Sensor 14',         null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:quality_sensor', x'000000000140f207a9e96900384139bf0a15544d08', 27582, 15, NULL, 'system',    'timeseries', 'Sensor quality',    null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     x'000000000140f207a9e96900384139bf0a15544d08', 27582, 16, NULL, 'system',    'timeseries', 'Sensor multi type', null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:17',             x'000000000140f207a9e96900384139bf0a15544d08', 27582, 17, NULL, 'system',    'profile',    'Sensor 17',         null, 3);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:18',             x'00000000014044000000000000c008000000000000',  4326, 18, NULL, 'system',    'timeseries', 'Sensor 18',         null, 3);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:19',             x'00000000014044000000000000c008000000000000',  4326, 19, NULL, 'system',    'timeseries', 'Sensor 19 No FOI',  null, 1);

INSERT INTO "om"."procedures_properties" ("id_procedure", "property_name", "value") VALUES ('urn:ogc:object:sensor:GEOM:2', 'bss-code',         '10972X0137/PONT');
INSERT INTO "om"."procedures_properties" ("id_procedure", "property_name", "value") VALUES ('urn:ogc:object:sensor:GEOM:2', 'bss-code',         'BSS10972X0137');
INSERT INTO "om"."procedures_properties" ("id_procedure", "property_name", "value") VALUES ('urn:ogc:object:sensor:GEOM:2', 'supervisor-code',  '00ARGLELES');
INSERT INTO "om"."procedures_properties" ("id_procedure", "property_name", "value") VALUES ('urn:ogc:object:sensor:GEOM:3', 'bss-code',         '10972X0137/SER');
INSERT INTO "om"."procedures_properties" ("id_procedure", "property_name", "value") VALUES ('urn:ogc:object:sensor:GEOM:3', 'bss-code',         'BSS10972X0137');

INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:2',              '2000-12-01 00:00:00.0', x'00000000014147600cde7df17fc13603c2c1e79f50', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:2',              '2000-12-11 00:00:00.0', x'0000000001414721e3e3c47123c1341d38f21784f0', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:2',              '2000-12-22 00:00:00.0', x'00000000014144f902f95b5e67c13b3ac452c1ca80', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:3',              '2007-05-01 02:59:00.0', x'00000000014044000000000000c008000000000000', 4326);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:4',              '2007-05-01 12:59:00.0', x'000000000140240000000000004024000000000000', 4326);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:test-1',         '2007-05-01 12:59:00.0', x'000000000140140000000000004024000000000000', 4326);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:7',              '2007-05-01 16:59:00.0', x'00000000014145b7ca31487fc1c138da59f139abf8', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:8',              '2007-05-01 12:59:00.0', x'000000000140efef0000000000413a6b2800000000', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:9',              '2009-05-01 13:47:00.0', x'000000000140efef0000000000413a6b2800000000', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:10',             '2009-05-01 13:47:00.0', x'000000000140efef0000000000413a6b2800000000', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:test-id',        '2009-05-01 13:47:00.0', x'000000000140efef0000000000413a6b2800000000', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:12',             '2000-12-01 00:00:00.0', x'000000000140240000000000004024000000000000', 4326);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:12',             '2000-12-11 00:00:00.0', x'000000000140240000000000004024000000000000', 4326);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:12',             '2000-12-22 00:00:00.0', x'000000000140240000000000004024000000000000', 4326);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:13',             '2000-01-01 00:00:00.0', x'00000000014044000000000000c008000000000000', 4326);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:14',             '2000-12-01 00:00:00.0', x'00000000014147600cde7df17fc13603c2c1e79f50', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:14',             '2000-12-11 00:00:00.0', x'0000000001414721e3e3c47123c1341d38f21784f0', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:14',             '2000-12-22 00:00:00.0', x'00000000014144f902f95b5e67c13b3ac452c1ca80', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:14',             '2000-12-24 00:00:00.0', x'00000000014144f902f95b5e67c13b3ac452c1ca80', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:quality_sensor', '1980-03-01 21:52:00.0', x'000000000140efef0000000000413a6b2800000000', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     '1980-03-01 21:52:00.0', x'000000000140efef0000000000413a6b2800000000', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:17',             '2000-01-01 00:00:00.0', x'000000000140efef0000000000413a6b2800000000', 27582);
INSERT INTO "om"."historical_locations" VALUES ('urn:ogc:object:sensor:GEOM:18',             '2000-01-01 00:00:00.0', x'00000000014044000000000000c008000000000000', 4326);


INSERT INTO "om"."sampling_features" VALUES ('station-001', '10972X0137-PONT' , 'Point d''eau BSSS', 'urn:-sandre:object:bdrhf:123X', x'000000000140efef0000000000413a6b2800000000', 27582);
INSERT INTO "om"."sampling_features" VALUES ('station-002', '10972X0137-PLOUF', 'Point d''eau BSSS', 'urn:-sandre:object:bdrhf:123X', x'000000000140140000000000004024000000000000',  4326);
INSERT INTO "om"."sampling_features" VALUES ('station-003', '66685X4587-WARP',  'Station Thermale',  'urn:-sandre:object:bdrhf:123X', x'000000000140f1490000000000413cdd4b00000000', 27582);
INSERT INTO "om"."sampling_features" VALUES ('station-004', '99917X9856-FRAG',  'Puits',             'urn:-sandre:object:bdrhf:123X', x'000000000140e47b20000000004143979980000000', 27582);
INSERT INTO "om"."sampling_features" VALUES ('station-005', '44499X4517-TRUC',  'bouee ds le rhone', 'urn:-sandre:object:bdrhf:123X', x'000000000140ee8480000000004138cc9400000000', 27582);
INSERT INTO "om"."sampling_features" VALUES ('station-006', 'cycle1',           'Geology traverse',   NULL,                           x'000000000200000007c03eb604189374bc4060c68f5c28f5c3c03eb5c28f5c28f64060c6872b020c4ac03eb5810624dd2f4060c67ef9db22d1c03eb53f7ced91684060c66e978d4fdfc03eb4bc6a7ef9db4060c645a1cac083c03eb3f7ced916874060c64dd2f1a9fcc03eb3b645a1cac14060c65e353f7cee', 27582);


INSERT INTO "om"."sampling_features_properties" ("id_sampling_feature", "property_name", "value") VALUES ('station-001', 'commune',  'Argeles');
INSERT INTO "om"."sampling_features_properties" ("id_sampling_feature", "property_name", "value") VALUES ('station-001', 'region',   'Occitanie');
INSERT INTO "om"."sampling_features_properties" ("id_sampling_feature", "property_name", "value") VALUES ('station-002', 'commune',  'Beziers');
INSERT INTO "om"."sampling_features_properties" ("id_sampling_feature", "property_name", "value") VALUES ('station-002', 'commune',  'Maraussan');

INSERT INTO "om"."offerings" VALUES ('offering-1',  NULL, 'offering-1',  NULL,                    NULL,                    'urn:ogc:object:sensor:GEOM:1');
INSERT INTO "om"."offerings" VALUES ('offering-2',  NULL, 'offering-2',  '2000-12-01 00:00:00.0', '2000-12-22 00:00:00.0', 'urn:ogc:object:sensor:GEOM:2');
INSERT INTO "om"."offerings" VALUES ('offering-3',  NULL, 'offering-3',  '2007-05-01 02:59:00.0', '2007-05-01 21:59:00.0', 'urn:ogc:object:sensor:GEOM:3');
INSERT INTO "om"."offerings" VALUES ('offering-4',  NULL, 'offering-4',  '2007-05-01 12:59:00.0', '2007-05-01 16:59:00.0', 'urn:ogc:object:sensor:GEOM:4');
INSERT INTO "om"."offerings" VALUES ('offering-5',  NULL, 'offering-5',  '2007-05-01 12:59:00.0', '2007-05-01 16:59:00.0', 'urn:ogc:object:sensor:GEOM:test-1');
INSERT INTO "om"."offerings" VALUES ('offering-6',  NULL, 'offering-6',  NULL,                    NULL,                    'urn:ogc:object:sensor:GEOM:6');
INSERT INTO "om"."offerings" VALUES ('offering-7',  NULL, 'offering-7',  '2007-05-01 16:59:00.0', NULL,                    'urn:ogc:object:sensor:GEOM:7');
INSERT INTO "om"."offerings" VALUES ('offering-8',  NULL, 'offering-8',  '2007-05-01 12:59:00.0', '2007-05-01 16:59:00.0', 'urn:ogc:object:sensor:GEOM:8');
INSERT INTO "om"."offerings" VALUES ('offering-9',  NULL, 'offering-9',  '2009-05-01 13:47:00.0', NULL,                    'urn:ogc:object:sensor:GEOM:9');
INSERT INTO "om"."offerings" VALUES ('offering-10', NULL, 'offering-10', '2009-05-01 13:47:00.0', '2009-05-01 14:04:00.0', 'urn:ogc:object:sensor:GEOM:10');
INSERT INTO "om"."offerings" VALUES ('offering-11', NULL, 'offering-11', '2009-05-01 13:47:00.0', '2009-05-01 14:03:00.0', 'urn:ogc:object:sensor:GEOM:test-id');
INSERT INTO "om"."offerings" VALUES ('offering-12', NULL, 'offering-12', '2000-12-01 00:00:00.0', '2012-12-22 00:00:00.0', 'urn:ogc:object:sensor:GEOM:12');
INSERT INTO "om"."offerings" VALUES ('offering-13', NULL, 'offering-13', '2000-01-01 00:00:00.0', '2001-01-01 00:00:00.0', 'urn:ogc:object:sensor:GEOM:13');
INSERT INTO "om"."offerings" VALUES ('offering-14', NULL, 'offering-14', '2000-12-01 00:00:00.0', '2000-12-24 00:00:00.0', 'urn:ogc:object:sensor:GEOM:14');
INSERT INTO "om"."offerings" VALUES ('offering-15', NULL, 'offering-15', '1980-03-01 21:52:00.0', '1984-03-01 21:52:00.0', 'urn:ogc:object:sensor:GEOM:quality_sensor');
INSERT INTO "om"."offerings" VALUES ('offering-16', NULL, 'offering-16', '1980-03-01 21:52:00.0', '1981-03-01 22:52:00.0', 'urn:ogc:object:sensor:GEOM:multi-type');
INSERT INTO "om"."offerings" VALUES ('offering-17', NULL, 'offering-17', '2000-01-01 00:00:00.0', '2000-01-03 00:00:00.0', 'urn:ogc:object:sensor:GEOM:17');
INSERT INTO "om"."offerings" VALUES ('offering-18', NULL, 'offering-18', '2000-01-01 00:00:00.0', '2001-01-01 00:00:00.0', 'urn:ogc:object:sensor:GEOM:18');
INSERT INTO "om"."offerings" VALUES ('offering-19', NULL, 'offering-19', '2000-01-01 00:00:00.0',  NULL,                   'urn:ogc:object:sensor:GEOM:19');

INSERT INTO "om"."offering_foi" VALUES ('offering-2', 'station-002');
INSERT INTO "om"."offering_foi" VALUES ('offering-3', 'station-001');
INSERT INTO "om"."offering_foi" VALUES ('offering-4', 'station-001');
INSERT INTO "om"."offering_foi" VALUES ('offering-5', 'station-002');
INSERT INTO "om"."offering_foi" VALUES ('offering-7', 'station-002');
INSERT INTO "om"."offering_foi" VALUES ('offering-8', 'station-006');
INSERT INTO "om"."offering_foi" VALUES ('offering-9', 'station-006');
INSERT INTO "om"."offering_foi" VALUES ('offering-10','station-001');
INSERT INTO "om"."offering_foi" VALUES ('offering-10','station-002');
INSERT INTO "om"."offering_foi" VALUES ('offering-11','station-001');
INSERT INTO "om"."offering_foi" VALUES ('offering-12','station-001');
INSERT INTO "om"."offering_foi" VALUES ('offering-13','station-002');
INSERT INTO "om"."offering_foi" VALUES ('offering-14','station-002');
INSERT INTO "om"."offering_foi" VALUES ('offering-15','station-001');
INSERT INTO "om"."offering_foi" VALUES ('offering-16','station-001');
INSERT INTO "om"."offering_foi" VALUES ('offering-17','station-001');
INSERT INTO "om"."offering_foi" VALUES ('offering-18','station-002');


INSERT INTO "om"."offering_observed_properties" VALUES ('offering-2','aggregatePhenomenon');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-2','depth');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-2','temperature');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-3','depth');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-4','depth');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-5','aggregatePhenomenon');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-5','depth');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-5','temperature');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-7','temperature');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-8','aggregatePhenomenon');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-8','depth');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-8','temperature');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-9','depth');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-10','depth');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-11','depth');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-12','aggregatePhenomenon-2');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-12','depth');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-12','temperature');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-12','salinity');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-13','aggregatePhenomenon');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-13','aggregatePhenomenon-2');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-13','depth');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-13','temperature');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-13','salinity');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-14','aggregatePhenomenon');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-14','aggregatePhenomenon-2');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-14','depth');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-14','temperature');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-14','salinity');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-15','depth');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-16','isHot');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-16','color');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-16','expiration');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-16','age');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-16','multi-type-phenomenon');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-17','depth');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-17','isHot');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-17','color');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-17','expiration');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-17','age');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-17','multi-type-phenprofile');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-18','aggregatePhenomenon');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-18','aggregatePhenomenon-2');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-18','depth');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-18','temperature');
INSERT INTO "om"."offering_observed_properties" VALUES ('offering-18','salinity');

INSERT INTO "om"."offering_observed_properties" VALUES ('offering-19','temperature');

-- profile observation
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:2',     2, '2000-12-01 00:00:00.0', '2000-12-22 00:00:00.0', 'aggregatePhenomenon',    'urn:ogc:object:sensor:GEOM:2',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:9',     9, '2009-05-01 13:47:00.0',  NULL,                   'depth',                  'urn:ogc:object:sensor:GEOM:9',             'station-006');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:14',   14, '2000-12-01 00:00:00.0', '2000-12-24 00:00:00.0', 'aggregatePhenomenon-2',  'urn:ogc:object:sensor:GEOM:14',            'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:17',   17, '2000-01-01 00:00:00.0', '2000-01-03 00:00:00.0', 'multi-type-phenprofile', 'urn:ogc:object:sensor:GEOM:17',            'station-001');

-- timeseries observation
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:3',     3, '2007-05-01 02:59:00.0', '2007-05-01 21:59:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:3',              'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:4',     4, '2007-05-01 12:59:00.0', '2007-05-01 16:59:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:4',              'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:5',     5, '2007-05-01 12:59:00.0', '2007-05-01 16:59:00.0', 'aggregatePhenomenon',   'urn:ogc:object:sensor:GEOM:test-1',         'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:8',     8, '2007-05-01 12:59:00.0', '2007-05-01 16:59:00.0', 'aggregatePhenomenon',   'urn:ogc:object:sensor:GEOM:8',              'station-006');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:7',     7, '2007-05-01 16:59:00.0', NULL,                    'temperature',           'urn:ogc:object:sensor:GEOM:7',              'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:10',   10, '2009-05-01 13:47:00.0', '2009-05-01 14:04:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:10',             'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:11',   11, '2009-05-01 13:47:00.0', '2009-05-01 14:03:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:test-id',        'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:12',   12, '2000-12-01 00:00:00.0', '2012-12-22 00:00:00.0', 'aggregatePhenomenon-2', 'urn:ogc:object:sensor:GEOM:12',             'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:13',   13, '2000-01-01 00:00:00.0', '2001-01-01 00:00:00.0', 'aggregatePhenomenon-2', 'urn:ogc:object:sensor:GEOM:13',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:15',   15, '1980-03-01 21:52:00.0', '1984-03-01 21:52:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:quality_sensor', 'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:16',   16, '1980-03-01 21:52:00.0', '1981-03-01 22:52:00.0', 'multi-type-phenomenon', 'urn:ogc:object:sensor:GEOM:multi-type',     'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:18',   18, '2000-01-01 00:00:00.0', '2001-01-01 00:00:00.0', 'aggregatePhenomenon-2', 'urn:ogc:object:sensor:GEOM:18',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:19',   19, '2000-11-01 00:00:00.0', NULL,                    'temperature',           'urn:ogc:object:sensor:GEOM:19',              NULL);

-- profile sensor fields
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:2',              1, 'z_value',     'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',    NULL,         1, 'z_value',              'MAIN');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:2',              2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',    NULL,         1, 'depth',                'MEASURE');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:2',              3, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',   NULL,         1, 'temperature',          'MEASURE');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:9',              1, 'z_value',     'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',    NULL,         1, 'z_value',              'MAIN');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:9',              2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',    NULL,         1, 'depth',                'MEASURE');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:14',             1, 'z_value',     'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',    NULL,         1, 'z_value',              'MAIN');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:14',             2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',    NULL,         1, 'depth',                'MEASURE');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:14',             3, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',   NULL,         1, 'temperature',          'MEASURE');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:14',             4, 'salinity',    'Quantity', 'urn:ogc:def:phenomenon:GEOM:salinity',     'msu',  NULL,         1, 'salinity',             'MEASURE');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             1, 'z_value',      'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,         1, 'z_value',              'MAIN');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             2, 'depth',        'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,         1, 'depth',                'MEASURE');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             3, 'isHot',        'Boolean',  'urn:ogc:def:phenomenon:GEOM:isHot',        NULL,  NULL,         1, 'isHot',                'MEASURE');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             1, 'isHot_qual',   'Boolean',  'urn:ogc:def:phenomenon:GEOM:isHot_qual',   NULL,  'isHot',      1, 'isHot_qual',           'QUALITY');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             4, 'color',           'Text',  'urn:ogc:def:phenomenon:GEOM:color',        NULL,  NULL,         1, 'color',                'MEASURE');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             1, 'color_qual',      'Text',  'urn:ogc:def:phenomenon:GEOM:color_qual',   NULL,  'color',      1, 'color_qual',           'QUALITY');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             5, 'expiration',      'Time',  'urn:ogc:data:time:iso8601',                NULL,  NULL,         1, 'expiration',           'MEASURE');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             1, 'expiration_qual', 'Time',  'urn:ogc:data:time:iso8601',                NULL,  'expiration', 1, 'expiration_qual',      'QUALITY');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             6, 'age',         'Quantity',  'urn:ogc:def:phenomenon:GEOM:age',          NULL,  NULL,         1, 'age',                  'MEASURE');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             1, 'age_qual',    'Quantity',  'urn:ogc:def:phenomenon:GEOM:age',          NULL,  'age',        1, 'age_qual',             'QUALITY');

-- timeseries sensor fields
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:3',              1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time',        NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:3',              2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth',       NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:4',              1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'time',        NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:4',              2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth',       NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:test-1',         1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time',        NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:test-1',         2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth',       NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:test-1',         3, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature', NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:8',              1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time',        NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:8',              2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth',       NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:8',              3, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature', NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:7',              1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time',        NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:7',              2, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature', NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:10',             1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time',        NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:10',             2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth',       NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:test-id',        1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time',        NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:test-id',        2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth',       NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:12',             1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time',        NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:12',             2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth',       NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:12',             3, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature', NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:12',             4, 'salinity',    'Quantity', 'urn:ogc:def:phenomenon:GEOM:salinity',     'msu', NULL,    1, 'salinity',    NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:13',             1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time',        NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:13',             2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth',       NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:13',             3, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature', NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:13',             4, 'salinity',    'Quantity', 'urn:ogc:def:phenomenon:GEOM:salinity',     'msu', NULL,    1, 'salinity',    NULL);

INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:quality_sensor', 1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time',        NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:quality_sensor', 2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth',       NULL);
-- no quality for now in mixed implementation INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:quality_sensor', 1, 'qflag',       'Text',     'urn:ogc:def:phenomenon:GEOM:quality_flag',  NULL, 'depth', 1, 'qflag',       'QUALITY');
-- no quality for now in mixed implementation INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:quality_sensor', 2, 'qres',        'Quantity', 'urn:ogc:def:phenomenon:GEOM:quality_result',NULL, 'depth', 1, 'qres',       'QUALITY');

INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time',        NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     2, 'isHot',       'Boolean',  'urn:ogc:def:phenomenon:GEOM:isHot',        NULL,  NULL,    1, 'isHot',       NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     3, 'color',       'Text',     'urn:ogc:def:phenomenon:GEOM:color',        NULL,  NULL,    1, 'color',       NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     4, 'expiration',  'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'expiration',  NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     5, 'age',         'Quantity', 'urn:ogc:def:phenomenon:GEOM:age',          NULL,  NULL,    1, 'age',         NULL);

INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:18',             1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time',        NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:18',             2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth',       NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:18',             3, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature', NULL);
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:18',             4, 'salinity',    'Quantity', 'urn:ogc:def:phenomenon:GEOM:salinity',     'msu', NULL,    1, 'salinity',    NULL);

INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:19',             1, 'time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time',          'MAIN');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:19',             2, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature',   'MEASURE');

CREATE TABLE "main"."flat_csv_data" (
	"time" TIMESTAMP NOT NULL,
	"z_value" DOUBLE,
	"latitude" DOUBLE,
	"longitude" DOUBLE,
	"thing_id" character varying(10000) NOT NULL,
	"thing_name" character varying(10000),
	"thing_desc" character varying(10000),
	"uom" character varying(10000),
	"obsprop_id" character varying(10000) NOT NULL,
	"obsprop_name" character varying(10000),
	"obsprop_desc" character varying(10000),
	"result" DOUBLE
);

-- we can't set a primary key because of the diversity of observation type:
-- profile    => CONSTRAINT FLAT_CSV_DATA_PK PRIMARY KEY ("thing_id","obsprop_id","time","z_value")
-- timeseries => CONSTRAINT FLAT_CSV_DATA_PK PRIMARY KEY ("thing_id","obsprop_id","time")

-- 201
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 12,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL, '°C', 'temperature', 'temperature', NULL, 18.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 12,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL,  'm', 'depth',       'depth',       NULL, 12);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 24,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL, '°C', 'temperature', 'temperature', NULL, 19.7);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 24,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL,  'm', 'depth',       'depth',       NULL, 24);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 48,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL, '°C', 'temperature', 'temperature', NULL, 21.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 48,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL,  'm', 'depth',       'depth',       NULL, 48);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 96,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL, '°C', 'temperature', 'temperature', NULL, 23.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 96,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL,  'm', 'depth',       'depth',       NULL, 96);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 192, NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL, '°C', 'temperature', 'temperature', NULL, 26.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 192, NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL,  'm', 'depth',       'depth',       NULL, 192);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 384, NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL, '°C', 'temperature', 'temperature', NULL, 31.4);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 384, NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL,  'm', 'depth',       'depth',       NULL, 384);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 768, NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL, '°C', 'temperature', 'temperature', NULL, 35.1);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00.0', 768, NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL,  'm', 'depth',       'depth',       NULL, 768);

-- 202
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00.0', 12,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL, '°C', 'temperature', 'temperature', NULL, 18.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00.0', 12,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL,  'm', 'depth',       'depth',       NULL, 12);

-- 203
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00.0', 12,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL, '°C', 'temperature', 'temperature', NULL, 18.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00.0', 12,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:2', 'Sensor 2', NULL,  'm', 'depth',       'depth',       NULL, 12);


-- 304
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 02:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 03:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 04:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 05:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 06:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.56);

-- 305
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 07:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 08:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 09:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 10:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 11:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.56);

-- intentionally inserted in bad temporal order
-- 307
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 21:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.55);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 17:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 18:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.55);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 19:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.55);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 20:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:3', 'Sensor 3', NULL, 'm', 'depth', 'depth', NULL, 6.55);


-- 507 
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 12:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:test-1', 'Sensor test 1', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 13:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:test-1', 'Sensor test 1', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 14:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:test-1', 'Sensor test 1', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 15:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:test-1', 'Sensor test 1', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 16:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:test-1', 'Sensor test 1', NULL, 'm', 'depth', 'depth', NULL, 6.56);

-- 406
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 12:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:4', 'Sensor 4', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 13:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:4', 'Sensor 4', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 14:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:4', 'Sensor 4', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 15:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:4', 'Sensor 4', NULL, 'm', 'depth', 'depth', NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 16:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:4', 'Sensor 4', NULL, 'm', 'depth', 'depth', NULL, 6.56);

-- 801 
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 12:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:8', 'Sensor 8', NULL, 'm',  'depth',       'depth',       NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 12:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:8', 'Sensor 8', NULL, '°C', 'temperature', 'temperature', NULL, 12.0);

INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 13:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:8', 'Sensor 8', NULL, 'm',  'depth',       'depth',       NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 13:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:8', 'Sensor 8', NULL, '°C', 'temperature', 'temperature', NULL, 13.0);

INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 14:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:8', 'Sensor 8', NULL, 'm',  'depth',       'depth',       NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 14:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:8', 'Sensor 8', NULL, '°C', 'temperature', 'temperature', NULL, 14.0);

INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 15:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:8', 'Sensor 8', NULL, 'm',  'depth',       'depth',       NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 15:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:8', 'Sensor 8', NULL, '°C', 'temperature', 'temperature', NULL, 15.0);

INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 16:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:8', 'Sensor 8', NULL, 'm',  'depth',       'depth',       NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 16:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:8', 'Sensor 8', NULL, '°C', 'temperature', 'temperature', NULL, 16.0);


-- 702 
INSERT INTO "main"."flat_csv_data" VALUES ('2007-05-01 16:59:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:7', 'Sensor 7', NULL, '°C', 'temperature', 'temperature', NULL, 6.56);


-- 901
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 13:47:00', 15.5,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:9', 'Sensor 9', NULL, 'm',  'depth',       'depth',       NULL, 15.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 13:47:00', 19.7,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:9', 'Sensor 9', NULL, 'm',  'depth',       'depth',       NULL, 19.7);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 13:47:00', 21.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:9', 'Sensor 9', NULL, 'm',  'depth',       'depth',       NULL, 21.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 13:47:00', 23.9,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:9', 'Sensor 9', NULL, 'm',  'depth',       'depth',       NULL, 23.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 13:47:00', 22.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:9', 'Sensor 9', NULL, 'm',  'depth',       'depth',       NULL, 22.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 13:47:00', 18.4,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:9', 'Sensor 9', NULL, 'm',  'depth',       'depth',       NULL, 18.4);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 13:47:00', 17.1,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:9', 'Sensor 9', NULL, 'm',  'depth',       'depth',       NULL, 17.1);


-- 1001
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 13:47:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:10', 'Sensor 10', NULL, 'm',  'depth',       'depth',       NULL, 4.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 14:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:10', 'Sensor 10', NULL, 'm',  'depth',       'depth',       NULL, 5.9);

-- 1002
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 14:01:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:10', 'Sensor 10', NULL, 'm',  'depth',       'depth',       NULL, 8.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 14:02:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:10', 'Sensor 10', NULL, 'm',  'depth',       'depth',       NULL, 7.8);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 14:03:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:10', 'Sensor 10', NULL, 'm',  'depth',       'depth',       NULL, 9.9);

-- 1003
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 14:04:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:10', 'Sensor 10', NULL, 'm',  'depth',       'depth',       NULL, 9.1);


-- 2000
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 13:47:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:test-id', 'Sensor test id', NULL, 'm',  'depth',       'depth',       NULL, 4.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 14:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:test-id', 'Sensor test id', NULL, 'm',  'depth',       'depth',       NULL, 5.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 14:01:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:test-id', 'Sensor test id', NULL, 'm',  'depth',       'depth',       NULL, 8.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 14:02:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:test-id', 'Sensor test id', NULL, 'm',  'depth',       'depth',       NULL, 7.8);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-05-01 14:03:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:test-id', 'Sensor test id', NULL, 'm',  'depth',       'depth',       NULL, 9.9);

-- 3000  
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, 'm',   'depth',       'depth',       NULL, 2.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, '°C',  'temperature', 'temperature', NULL, 98.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, 'msu', 'salinity',    'salinity',    NULL, 4);

INSERT INTO "main"."flat_csv_data" VALUES ('2009-12-01 14:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, 'm',   'depth',       'depth',       NULL, 5.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-12-01 14:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, '°C',  'temperature', 'temperature', NULL, 1.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-12-01 14:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, 'msu', 'salinity',    'salinity',    NULL, 3);

INSERT INTO "main"."flat_csv_data" VALUES ('2009-12-11 14:01:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, 'm',   'depth',       'depth',       NULL, 8.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-12-11 14:01:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, '°C',  'temperature', 'temperature', NULL, 78.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-12-11 14:01:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, 'msu', 'salinity',    'salinity',    NULL, 2);

INSERT INTO "main"."flat_csv_data" VALUES ('2009-12-15 14:02:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, 'm',   'depth',       'depth',       NULL, 7.8);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-12-15 14:02:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, '°C',  'temperature', 'temperature', NULL, 14.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2009-12-15 14:02:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, 'msu', 'salinity',    'salinity',    NULL, 1);

INSERT INTO "main"."flat_csv_data" VALUES ('2012-12-22 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, 'm',   'depth',       'depth',       NULL, 9.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2012-12-22 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, '°C',  'temperature', 'temperature', NULL, 5.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2012-12-22 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:12', 'Sensor 12', NULL, 'msu', 'salinity',    'salinity',    NULL, 0);

-- 4XXX inserted in time disorder on purpose

-- 4000 
INSERT INTO "main"."flat_csv_data" VALUES ('2000-05-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'm',   'depth',       'depth',       NULL, 4.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-06-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'm',   'depth',       'depth',       NULL, 5.0);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-07-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'm',   'depth',       'depth',       NULL, 5.1);

-- 4001
INSERT INTO "main"."flat_csv_data" VALUES ('2000-01-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'm',   'depth',       'depth',       NULL, 4.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-01-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, '°C',  'temperature', 'temperature', NULL, 98.5);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-02-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'm',   'depth',       'depth',       NULL, 4.6);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-02-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, '°C',  'temperature', 'temperature', NULL, 97.5);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-03-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'm',   'depth',       'depth',       NULL, 4.7);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-03-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, '°C',  'temperature', 'temperature', NULL, 97.5);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-04-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'm',   'depth',       'depth',       NULL, 4.8);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-04-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, '°C',  'temperature', 'temperature', NULL, 96.5);

-- 4002
INSERT INTO "main"."flat_csv_data" VALUES ('2000-08-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'm',   'depth',       'depth',       NULL, 5.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-08-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, '°C',  'temperature', 'temperature', NULL, 98.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-08-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'msu', 'salinity',    'salinity',    NULL, 1.1);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-09-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'm',   'depth',       'depth',       NULL, 5.3);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-09-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, '°C',  'temperature', 'temperature', NULL, 87.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-09-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'msu', 'salinity',    'salinity',    NULL, 1.1);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-10-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'm',   'depth',       'depth',       NULL, 5.4);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-10-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, '°C',  'temperature', 'temperature', NULL, 77.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-10-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, 'msu', 'salinity',    'salinity',    NULL, 1.3);

-- 4003
INSERT INTO "main"."flat_csv_data" VALUES ('2000-11-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, '°C',  'temperature', 'temperature', NULL, 96.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, '°C',  'temperature', 'temperature', NULL, 99.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2001-01-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:13', 'Sensor 13', NULL, '°C',  'temperature', 'temperature', NULL, 96.5);


-- 5001
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 18.5,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 18.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 18.5,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.8);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 19.7,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 19.7);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 19.7,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.7);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 21.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 21.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 21.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.6);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 23.9,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 23.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 23.9,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.5);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 24.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 24.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 24.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.4);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 29.4,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 29.4);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 29.4,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.3);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 31.1,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 31.1);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', 31.1,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.2);

-- 5002
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 18.5,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 18.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 18.5,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.8);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 19.7,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 19.7);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 19.7,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.9);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 21.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 21.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 21.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 13.0);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 23.9,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 23.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 23.9,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 13.1);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 24.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 24.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 24.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 13.2);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 29.4,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 29.4);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 29.4,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 13.3);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 31.1,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 31.1);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-11 00:00:00', 31.1,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 13.4);

-- 5003
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 18.5,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 18.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 18.5,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.8);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 18.5,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 5.1);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 19.7,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 19.7);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 19.7,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.7);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 19.7,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 5.2);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 21.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 21.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 21.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.6);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 21.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 5.3);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 23.9,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 23.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 23.9,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 23.9,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 5.4);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 24.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 24.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 24.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.4);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 24.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 5.5);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 29.4,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 29.4);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 29.4,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.3);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 29.4,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 5.6);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 31.1,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 31.1);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 31.1,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-22 00:00:00', 31.1,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 5.7);

-- 5004
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 18.5,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 18.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 18.5,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.8);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 18.5,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 5.1);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 19.7,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 19.7);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 19.7,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 12.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 19.7,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 5.0);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 21.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 21.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 21.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 13.0);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 21.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 4.9);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 23.9,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 23.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 23.9,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 13.1);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 23.9,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 4.8);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 24.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 24.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 24.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 13.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 24.2,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 4.7);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 29.4,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 29.4);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 29.4,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 13.3);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 29.4,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 4.6);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 31.1,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'm',   'depth',       'depth',       NULL, 31.1);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 31.1,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, '°C',  'temperature', 'temperature', NULL, 13.4);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-24 00:00:00', 31.1,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:14', 'Sensor 14', NULL, 'msu', 'salinity',    'salinity',    NULL, 4.5);


-- 6001 quality flags are not yet supported by this implementation

INSERT INTO "main"."flat_csv_data" VALUES ('1980-03-01 21:52:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:quality_sensor', 'Sensor quality', NULL, 'm',   'depth',       'depth',       NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('1981-03-01 21:52:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:quality_sensor', 'Sensor quality', NULL, 'm',   'depth',       'depth',       NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('1982-03-01 21:52:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:quality_sensor', 'Sensor quality', NULL, 'm',   'depth',       'depth',       NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('1983-03-01 21:52:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:quality_sensor', 'Sensor quality', NULL, 'm',   'depth',       'depth',       NULL, 6.56);
INSERT INTO "main"."flat_csv_data" VALUES ('1984-03-01 21:52:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:quality_sensor', 'Sensor quality', NULL, 'm',   'depth',       'depth',       NULL, 6.56);

--CREATE TABLE "mesures"."mesure15"("id_observation"      integer NOT NULL,
--                                  "id"                  integer NOT NULL,
--                                  "Time"                timestamp NOT NULL,
--                                  "depth"               double,
--                                  "depth_quality_qflag" character varying(1000),
--                                  "depth_quality_qres"  double); 
--
--INSERT INTO "mesures"."mesure15" VALUES (6001, 1,  '1980-03-01 21:52:00', 6.56, 'ok', 3.1);
--INSERT INTO "mesures"."mesure15" VALUES (6001, 2,  '1981-03-01 21:52:00', 6.56, 'ko', 3.2);
--INSERT INTO "mesures"."mesure15" VALUES (6001, 3,  '1982-03-01 21:52:00', 6.56, 'ok', 3.3);
--INSERT INTO "mesures"."mesure15" VALUES (6001, 4,  '1983-03-01 21:52:00', 6.56, 'ko', 3.4);
--INSERT INTO "mesures"."mesure15" VALUES (6001, 5,  '1984-03-01 21:52:00', 6.56, 'ok', 3.5);
--
--
---- 7001 only double field are supported for now in this implementation
--CREATE TABLE "mesures"."mesure16"("id_observation"      integer NOT NULL,
--                                  "id"                  integer NOT NULL,
--                                  "Time"                timestamp NOT NULL,
--                                  "isHot"               integer,
--                                  "color"               character varying(10000),
--                                  "expiration"          timestamp,
--                                  "age"                 double); 
--
--INSERT INTO "mesures"."mesure16" VALUES (7001, 1,  '1980-03-01 21:52:00', 0, 'blue', '2000-01-01 22:10:00', 12);
--INSERT INTO "mesures"."mesure16" VALUES (7001, 2,  '1981-03-01 22:52:00', 1, 'red',  '2001-01-01 22:10:00', 14);
--
--
---- 8001/8002/8003 only double field are supported for now in this implementation + quality flags are not yet supported by this implementation
--
--
--CREATE TABLE "mesures"."mesure17"("id_observation"             integer NOT NULL,
--                                  "id"                        integer NOT NULL,
--                                  "depth"                     double,
--                                  "isHot"                     integer,
--                                  "isHot_quality_isHot_qual"  integer); 
--
--INSERT INTO "mesures"."mesure17" VALUES (8001, 1,  1.0, 0, 0);
--INSERT INTO "mesures"."mesure17" VALUES (8001, 2,  2.0, 1, 0);
--INSERT INTO "mesures"."mesure17" VALUES (8001, 3,  3.0, 0, 1);
--
--INSERT INTO "mesures"."mesure17" VALUES (8002, 1,  1.0, 1, 1);
--INSERT INTO "mesures"."mesure17" VALUES (8002, 2,  2.0, 1, 1);
--INSERT INTO "mesures"."mesure17" VALUES (8002, 3,  3.0, 1, 1);
--
--INSERT INTO "mesures"."mesure17" VALUES (8003, 1,  1.0, 0, 0);
--INSERT INTO "mesures"."mesure17" VALUES (8003, 2,  2.0, 0, 0);
--INSERT INTO "mesures"."mesure17" VALUES (8003, 3,  3.0, 0, 0);
--
--CREATE TABLE "mesures"."mesure17_2"("id_observation"                     integer NOT NULL,
--                                    "id"                                 integer NOT NULL,
--                                    "color"                              character varying(10000),
--                                    "color_quality_color_qual"           character varying(10000),
--                                    "expiration"                         timestamp,
--                                    "expiration_quality_expiration_qual"  timestamp); 
--
--INSERT INTO "mesures"."mesure17_2" VALUES (8001, 1, 'blue',  'good', '2000-01-01 22:00:00', '2000-01-01 23:00:00');
--INSERT INTO "mesures"."mesure17_2" VALUES (8001, 2, 'green', 'fade', '2000-01-01 22:00:00', '2000-01-01 23:00:00');
--INSERT INTO "mesures"."mesure17_2" VALUES (8001, 3, 'red',   'bad',  '2000-01-01 22:00:00', '2000-01-01 23:00:00');
--
--INSERT INTO "mesures"."mesure17_2" VALUES (8002, 1, 'yellow',  'good', '2000-01-02 22:00:00', '2000-01-02 23:00:00');
--INSERT INTO "mesures"."mesure17_2" VALUES (8002, 2, 'yellow',  'good', '2000-01-02 22:00:00', '2000-01-02 23:00:00');
--INSERT INTO "mesures"."mesure17_2" VALUES (8002, 3, 'yellow',  'good', '2000-01-02 22:00:00', '2000-01-02 23:00:00');
--
--INSERT INTO "mesures"."mesure17_2" VALUES (8003, 1, 'brown',  'bad',  '2000-01-03 22:00:00', '2000-01-03 23:00:00');
--INSERT INTO "mesures"."mesure17_2" VALUES (8003, 2, 'black',  'fade', '2000-01-03 22:00:00', '2000-01-03 23:00:00');
--INSERT INTO "mesures"."mesure17_2" VALUES (8003, 3, 'black',  'fade', '2000-01-03 22:00:00', '2000-01-03 23:00:00');
--
--CREATE TABLE "mesures"."mesure17_3"("id_observation"      integer NOT NULL,
--                                    "id"                  integer NOT NULL,
--                                    "age"                 double,
--                                    "age_quality_age_qual" double); 
--
--INSERT INTO "mesures"."mesure17_3" VALUES (8001, 1,  27.0, 37.0);
--INSERT INTO "mesures"."mesure17_3" VALUES (8001, 2,  28.0, 38.0);
--INSERT INTO "mesures"."mesure17_3" VALUES (8001, 3,  29.0, 39.1);
--
--INSERT INTO "mesures"."mesure17_3" VALUES (8002, 1,  16.3, 16.3);
--INSERT INTO "mesures"."mesure17_3" VALUES (8002, 2,  26.4, 25.4);
--INSERT INTO "mesures"."mesure17_3" VALUES (8002, 3,  30.0, 28.1);
--
--INSERT INTO "mesures"."mesure17_3" VALUES (8003, 1,  11.0, 0.0);
--INSERT INTO "mesures"."mesure17_3" VALUES (8003, 2,  22.0, 0.0);
--INSERT INTO "mesures"."mesure17_3" VALUES (8003, 3,  33.0, 0.0);

-- 9XXX inserted in time disorder on purpose

-- 9000 
INSERT INTO "main"."flat_csv_data" VALUES ('2000-05-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'm',   'depth',       'depth',       NULL, 4.9);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-06-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'm',   'depth',       'depth',       NULL, 5.0);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-07-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'm',   'depth',       'depth',       NULL, 5.1);

-- 9001 
INSERT INTO "main"."flat_csv_data" VALUES ('2000-01-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'm',   'depth',       'depth',       NULL, 4.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-01-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, '°C',  'temperature', 'temperature', NULL, 98.5);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-02-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'm',   'depth',       'depth',       NULL, 4.6);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-02-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, '°C',  'temperature', 'temperature', NULL, 97.5);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-03-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'm',   'depth',       'depth',       NULL, 4.7);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-03-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, '°C',  'temperature', 'temperature', NULL, 97.5);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-04-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'm',   'depth',       'depth',       NULL, 4.8);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-04-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, '°C',  'temperature', 'temperature', NULL, 96.5);

-- 9002
INSERT INTO "main"."flat_csv_data" VALUES ('2000-08-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'm',   'depth',       'depth',       NULL, 5.2);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-08-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, '°C',  'temperature', 'temperature', NULL, 98.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-08-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'msu', 'salinity',    'salinity',    NULL, 1.1);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-09-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'm',   'depth',       'depth',       NULL, 5.3);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-09-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, '°C',  'temperature', 'temperature', NULL, 87.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-09-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'msu', 'salinity',    'salinity',    NULL, 1.1);

INSERT INTO "main"."flat_csv_data" VALUES ('2000-10-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'm',   'depth',       'depth',       NULL, 5.4);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-10-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, '°C',  'temperature', 'temperature', NULL, 77.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-10-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, 'msu', 'salinity',    'salinity',    NULL, 1.3);

-- 9003
INSERT INTO "main"."flat_csv_data" VALUES ('2000-11-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, '°C',  'temperature', 'temperature', NULL, 96.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2000-12-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, '°C',  'temperature', 'temperature', NULL, 99.5);
INSERT INTO "main"."flat_csv_data" VALUES ('2001-01-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:18', 'Sensor 18', NULL, '°C',  'temperature', 'temperature', NULL, 96.5);

-- 9004
INSERT INTO "main"."flat_csv_data" VALUES ('2000-01-01 00:00:00', NULL,  NULL, NULL, 'urn:ogc:object:sensor:GEOM:19', 'Sensor 19 No FOI', NULL, '°C',  'temperature', 'temperature', NULL, 6.6);