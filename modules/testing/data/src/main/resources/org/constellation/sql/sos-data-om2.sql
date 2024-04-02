INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('depth',                 'Depth',                             'urn:ogc:def:phenomenon:GEOM:depth',                 'the depth in water');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('temperature',           'Temperature',                       'urn:ogc:def:phenomenon:GEOM:temperature',           'the temperature in celcius degree');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('aggregatePhenomenon',   'Aggregate Phenomenon',               'urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon',   'the aggregation of temperature and depth phenomenons');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('salinity',              'Salinity',                          'urn:ogc:def:phenomenon:GEOM:salinity',              'the salinity in water');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('aggregatePhenomenon-2', 'Aggregate Phenomenon 2',             'urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon-2', 'the aggregation of temperature depth, and salinity phenomenons');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('isHot',                 'Hotness',                           'urn:ogc:def:phenomenon:GEOM:isHot',                 'hotness indicator');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('color',                 'Color',                             'urn:ogc:def:phenomenon:GEOM:color',                 'the color label');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('expiration',            'Expiration Date',                    'urn:ogc:def:phenomenon:GEOM:expiration',            'Expiration date');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('age',                   'Age',                               'urn:ogc:def:phenomenon:GEOM:age',                   'current age');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('multi-type-phenomenon',  'Multi type phenomenon',             'urn:ogc:def:phenomenon:GEOM:multi-type-phenomenon', 'the aggregation of variable phenomenons type');
INSERT INTO "om"."observed_properties" ("id", "name", "definition", "description") VALUES ('multi-type-phenprofile', 'Multi type phenomenon for Profile', 'urn:ogc:def:phenomenon:GEOM:multi-type-phenprofile', 'the aggregation of variable phenomenons type for profile');

INSERT INTO "om"."observed_properties_properties" ("id_phenomenon", "property_name", "value") VALUES ('aggregatePhenomenon',   'phen-category',  'physics');
INSERT INTO "om"."observed_properties_properties" ("id_phenomenon", "property_name", "value") VALUES ('aggregatePhenomenon-2', 'phen-category',  'elementary');
INSERT INTO "om"."observed_properties_properties" ("id_phenomenon", "property_name", "value") VALUES ('depth',                'phen-category',  'biological');
INSERT INTO "om"."observed_properties_properties" ("id_phenomenon", "property_name", "value") VALUES ('depth',                'phen-category',  'organics');
INSERT INTO "om"."observed_properties_properties" ("id_phenomenon", "property_name", "value") VALUES ('depth',                'phen-usage',     'production');
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
---------

INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:1',              x'000000000140efef0000000000413a6b2800000000', 27582, 1,  NULL, 'system',    'timeseries', 'Sensor 1',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:2',              x'000000000140f207a9e96900384139bf0a15544d08', 27582, 2,  NULL, 'component', 'profile',    'Sensor 2',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:3',              x'00000000014044000000000000c008000000000000', 4326,  3,  NULL, 'system',    'timeseries', 'Sensor 3',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:4',              x'000000000140240000000000004024000000000000', 4326,  4,  NULL, 'system',    'timeseries', 'Sensor 4',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:test-1',         x'000000000140140000000000004024000000000000', 4326,  5,  NULL, 'system',    'timeseries', 'Sensor test 1',     null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:6',              x'000000000140140000000000004014000000000000', 4326,  6,  NULL, 'system',    'timeseries', 'Sensor 6',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:7',              x'00000000014145b7ca31487fc1c138da59f139abf8', 27582, 7,  NULL, 'system',    'timeseries', 'Sensor 7',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:8',              x'000000000140efef0000000000413a6b2800000000', 27582, 8,  NULL, 'system',    'timeseries', 'Sensor 8',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:9',              x'000000000140efef0000000000413a6b2800000000', 27582, 9,  NULL, 'system',    'profile',    'Sensor 9',          null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:10',             x'000000000140efef0000000000413a6b2800000000', 27582, 10, NULL, 'system',    'timeseries', 'Sensor 10',         null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:test-id',        x'000000000140efef0000000000413a6b2800000000', 27582, 11, NULL, 'system',    'timeseries', 'Sensor test id',    null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:12',             x'000000000140efef0000000000413a6b2800000000', 27582, 12, NULL, 'system',    'timeseries', 'Sensor 12',         null, 2);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:13',             x'00000000014044000000000000c008000000000000',  4326, 13, NULL, 'system',    'timeseries', 'Sensor 13',         null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:14',             x'000000000140f207a9e96900384139bf0a15544d08', 27582, 14, NULL, 'system',    'profile',    'Sensor 14',         null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:quality_sensor', x'000000000140f207a9e96900384139bf0a15544d08', 27582, 15, NULL, 'system',    'timeseries',    'Sensor quality',    null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     x'000000000140f207a9e96900384139bf0a15544d08', 27582, 16, NULL, 'system',    'timeseries', 'Sensor multi type', null, 1);
INSERT INTO "om"."procedures" VALUES ('urn:ogc:object:sensor:GEOM:17',             x'000000000140f207a9e96900384139bf0a15544d08', 27582, 17, NULL, 'system',    'profile',    'Sensor 17',         null, 3);

INSERT INTO "om"."procedures_properties" ("id_procedure", "property_name", "value") VALUES ('urn:ogc:object:sensor:GEOM:2', 'bss-code',         '10972X0137/PONT');
INSERT INTO "om"."procedures_properties" ("id_procedure", "property_name", "value") VALUES ('urn:ogc:object:sensor:GEOM:2', 'bss-code',         'BSS10972X0137');
INSERT INTO "om"."procedures_properties" ("id_procedure", "property_name", "value") VALUES ('urn:ogc:object:sensor:GEOM:2', 'supervisor-code',   '00ARGLELES');
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
INSERT INTO "om"."offerings" VALUES ('offering-13', NULL, 'offering-13', '2000-01-01 00:00:00.0', NULL,                    'urn:ogc:object:sensor:GEOM:13');
INSERT INTO "om"."offerings" VALUES ('offering-14', NULL, 'offering-14', '2000-12-01 00:00:00.0', '2000-12-24 00:00:00.0', 'urn:ogc:object:sensor:GEOM:14');
INSERT INTO "om"."offerings" VALUES ('offering-15', NULL, 'offering-15', '1980-03-01 21:52:00.0', '1984-03-01 21:52:00.0', 'urn:ogc:object:sensor:GEOM:quality_sensor');
INSERT INTO "om"."offerings" VALUES ('offering-16', NULL, 'offering-16', '1980-03-01 21:52:00.0', '1981-03-01 22:52:00.0', 'urn:ogc:object:sensor:GEOM:multi-type');
INSERT INTO "om"."offerings" VALUES ('offering-17', NULL, 'offering-17', '2000-01-01 00:00:00.0', '2000-01-03 00:00:00.0', 'urn:ogc:object:sensor:GEOM:17');

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

INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:201',   201, '2000-12-01 00:00:00.0', NULL,                    'aggregatePhenomenon',   'urn:ogc:object:sensor:GEOM:2',              'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:202',   202, '2000-12-11 00:00:00.0', NULL,                    'aggregatePhenomenon',   'urn:ogc:object:sensor:GEOM:2',              'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:203',   203, '2000-12-22 00:00:00.0', NULL,                    'aggregatePhenomenon',   'urn:ogc:object:sensor:GEOM:2',              'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:406',   406, '2007-05-01 12:59:00.0', '2007-05-01 16:59:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:4',              'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:304',   304, '2007-05-01 02:59:00.0', '2007-05-01 06:59:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:3',              'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:305',   305, '2007-05-01 07:59:00.0', '2007-05-01 11:59:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:3',              'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:307',   307, '2007-05-01 17:59:00.0', '2007-05-01 21:59:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:3',              'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:507',   507, '2007-05-01 12:59:00.0', '2007-05-01 16:59:00.0', 'aggregatePhenomenon',   'urn:ogc:object:sensor:GEOM:test-1',         'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:801',   801, '2007-05-01 12:59:00.0', '2007-05-01 16:59:00.0', 'aggregatePhenomenon',   'urn:ogc:object:sensor:GEOM:8',              'station-006');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:702',   702, '2007-05-01 16:59:00.0', NULL,                    'temperature',           'urn:ogc:object:sensor:GEOM:7',              'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:901',   901, '2009-05-01 13:47:00.0', NULL,                    'depth',                 'urn:ogc:object:sensor:GEOM:9',              'station-006');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:1001', 1001, '2009-05-01 13:47:00.0', '2009-05-01 14:00:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:10',             'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:1002', 1002, '2009-05-01 14:01:00.0', '2009-05-01 14:03:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:10',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:1003', 1003, '2009-05-01 14:04:00.0', NULL,                    'depth',                 'urn:ogc:object:sensor:GEOM:10',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:2000', 2000, '2009-05-01 13:47:00.0', '2009-05-01 14:03:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:test-id',        'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:3000', 3000, '2000-12-01 00:00:00.0', '2012-12-22 00:00:00.0', 'aggregatePhenomenon-2', 'urn:ogc:object:sensor:GEOM:12',             'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:4000', 4000, '2000-05-01 00:00:00.0', '2000-07-01 00:00:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:13',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:4001', 4001, '2000-01-01 00:00:00.0', '2000-04-01 00:00:00.0', 'aggregatePhenomenon',   'urn:ogc:object:sensor:GEOM:13',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:4002', 4002, '2000-08-01 00:00:00.0', '2000-10-01 00:00:00.0', 'aggregatePhenomenon-2', 'urn:ogc:object:sensor:GEOM:13',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:4003', 4003, '2000-11-01 00:00:00.0', '2001-01-01 00:00:00.0', 'temperature',           'urn:ogc:object:sensor:GEOM:13',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:5001', 5001, '2000-12-01 00:00:00.0', NULL,                    'aggregatePhenomenon',   'urn:ogc:object:sensor:GEOM:14',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:5002', 5002, '2000-12-11 00:00:00.0', NULL,                    'aggregatePhenomenon',   'urn:ogc:object:sensor:GEOM:14',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:5003', 5003, '2000-12-22 00:00:00.0', NULL,                    'aggregatePhenomenon-2', 'urn:ogc:object:sensor:GEOM:14',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:5004', 5004, '2000-12-24 00:00:00.0', NULL,                    'aggregatePhenomenon-2', 'urn:ogc:object:sensor:GEOM:14',             'station-002');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:6001', 6001, '1980-03-01 21:52:00.0', '1984-03-01 21:52:00.0', 'depth',                 'urn:ogc:object:sensor:GEOM:quality_sensor', 'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:7001', 7001, '1980-03-01 21:52:00.0', '1981-03-01 22:52:00.0', 'multi-type-phenomenon', 'urn:ogc:object:sensor:GEOM:multi-type',     'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:8001', 8001, '2000-01-01 00:00:00.0', NULL,                    'multi-type-phenprofile', 'urn:ogc:object:sensor:GEOM:17',             'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:8002', 8002, '2000-01-02 00:00:00.0', NULL,                    'multi-type-phenprofile', 'urn:ogc:object:sensor:GEOM:17',             'station-001');
INSERT INTO "om"."observations"  VALUES ('urn:ogc:object:observation:GEOM:8003', 8003, '2000-01-03 00:00:00.0', NULL,                    'multi-type-phenprofile', 'urn:ogc:object:sensor:GEOM:17',             'station-001');

INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:2',              1, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:2',              2, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:3',              1, 'Time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:3',              2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:4',              1, 'Time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'time');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:4',              2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:test-1',         1, 'Time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:test-1',         2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:test-1',         3, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:8',              1, 'Time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:8',              2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:8',              3, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:7',              1, 'Time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:7',              2, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:9',              1, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:10',             1, 'Time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:10',             2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:test-id',        1, 'Time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:test-id',        2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:12',             1, 'Time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:12',             2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:12',             3, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:12',             4, 'salinity',    'Quantity', 'urn:ogc:def:phenomenon:GEOM:salinity',     'msu', NULL,    2, 'salinity');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:13',             1, 'Time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:13',             2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:13',             3, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:13',             4, 'salinity',    'Quantity', 'urn:ogc:def:phenomenon:GEOM:salinity',     'msu', NULL,    1, 'salinity');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:14',             1, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:14',             2, 'temperature', 'Quantity', 'urn:ogc:def:phenomenon:GEOM:temperature',  '°C',  NULL,    1, 'temperature');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:14',             3, 'salinity',    'Quantity', 'urn:ogc:def:phenomenon:GEOM:salinity',     'msu', NULL,    1, 'salinity');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:quality_sensor', 1, 'Time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:quality_sensor', 2, 'depth',       'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,    1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:quality_sensor', 1, 'qflag',       'Text',     'urn:ogc:def:phenomenon:GEOM:quality_flag',  NULL, 'depth', 1, 'qflag');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:quality_sensor', 2, 'qres',        'Quantity', 'urn:ogc:def:phenomenon:GEOM:quality_result',NULL, 'depth', 1, 'qres');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     1, 'Time',        'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'Time');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     2, 'isHot',       'Boolean',  'urn:ogc:def:phenomenon:GEOM:isHot',        NULL,  NULL,    1, 'isHot');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     3, 'color',       'Text',     'urn:ogc:def:phenomenon:GEOM:color',        NULL,  NULL,    1, 'color');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     4, 'expiration',  'Time',     'urn:ogc:data:time:iso8601',                NULL,  NULL,    1, 'expiration');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:multi-type',     5, 'age',         'Quantity', 'urn:ogc:def:phenomenon:GEOM:age',          NULL,  NULL,    1, 'age');

INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             1, 'depth',        'Quantity', 'urn:ogc:def:phenomenon:GEOM:depth',        'm',   NULL,         1, 'depth');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             2, 'isHot',        'Boolean',  'urn:ogc:def:phenomenon:GEOM:isHot',        NULL,  NULL,         1, 'isHot');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             1, 'isHot_qual',   'Boolean',  'urn:ogc:def:phenomenon:GEOM:isHot_qual',   NULL,  'isHot',      1, 'isHot_qual');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             3, 'color',           'Text',  'urn:ogc:def:phenomenon:GEOM:color',        NULL,  NULL,         2, 'color');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             1, 'color_qual',      'Text',  'urn:ogc:def:phenomenon:GEOM:color_qual',   NULL,  'color',      2, 'color_qual');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             4, 'expiration',      'Time',  'urn:ogc:data:time:iso8601',                NULL,  NULL,         2, 'expiration');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             1, 'expiration_qual', 'Time',  'urn:ogc:data:time:iso8601',                NULL,  'expiration', 2, 'expiration_qual');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             5, 'age',         'Quantity',  'urn:ogc:def:phenomenon:GEOM:age',          NULL,  NULL,         3, 'age');
INSERT INTO "om"."procedure_descriptions"  VALUES ('urn:ogc:object:sensor:GEOM:17',             5, 'age_qual',    'Quantity',  'urn:ogc:def:phenomenon:GEOM:age',          NULL,  'age',        3, 'age_qual');

CREATE TABLE "mesures"."mesure2"("id_observation" integer NOT NULL,
                                 "id"             integer NOT NULL,
                                 "depth"          double,
                                 "temperature"    double);

INSERT INTO "mesures"."mesure2" VALUES (201, 1, 12,  18.5);
INSERT INTO "mesures"."mesure2" VALUES (201, 2, 24,  19.7);
INSERT INTO "mesures"."mesure2" VALUES (201, 3, 48,  21.2);
INSERT INTO "mesures"."mesure2" VALUES (201, 4, 96,  23.9);
INSERT INTO "mesures"."mesure2" VALUES (201, 5, 192, 26.2);
INSERT INTO "mesures"."mesure2" VALUES (201, 6, 384, 31.4);
INSERT INTO "mesures"."mesure2" VALUES (201, 7, 768, 35.1);

INSERT INTO "mesures"."mesure2" VALUES (202, 1, 12,  18.5);

INSERT INTO "mesures"."mesure2" VALUES (203, 1, 12,  18.5);

CREATE TABLE "mesures"."mesure3"("id_observation" integer NOT NULL,
                                 "id"             integer NOT NULL,
                                 "Time"           timestamp,
                                 "depth"          double);

INSERT INTO "mesures"."mesure3" VALUES (304, 1, '2007-05-01 02:59:00',6.56);
INSERT INTO "mesures"."mesure3" VALUES (304, 2, '2007-05-01 03:59:00',6.56);
INSERT INTO "mesures"."mesure3" VALUES (304, 3, '2007-05-01 04:59:00',6.56);
INSERT INTO "mesures"."mesure3" VALUES (304, 4, '2007-05-01 05:59:00',6.56);
INSERT INTO "mesures"."mesure3" VALUES (304, 5, '2007-05-01 06:59:00',6.56);

INSERT INTO "mesures"."mesure3" VALUES (305, 1, '2007-05-01 07:59:00',6.56);
INSERT INTO "mesures"."mesure3" VALUES (305, 2, '2007-05-01 08:59:00',6.56);
INSERT INTO "mesures"."mesure3" VALUES (305, 3, '2007-05-01 09:59:00',6.56);
INSERT INTO "mesures"."mesure3" VALUES (305, 4, '2007-05-01 10:59:00',6.56);
INSERT INTO "mesures"."mesure3" VALUES (305, 5, '2007-05-01 11:59:00',6.56);

-- intentionally inserted in bad temporal order
INSERT INTO "mesures"."mesure3" VALUES (307, 1, '2007-05-01 21:59:00',6.55);
INSERT INTO "mesures"."mesure3" VALUES (307, 2, '2007-05-01 17:59:00',6.56);
INSERT INTO "mesures"."mesure3" VALUES (307, 3, '2007-05-01 18:59:00',6.55);
INSERT INTO "mesures"."mesure3" VALUES (307, 4, '2007-05-01 19:59:00',6.55);
INSERT INTO "mesures"."mesure3" VALUES (307, 5, '2007-05-01 20:59:00',6.55);

CREATE TABLE "mesures"."mesure5"("id_observation" integer NOT NULL,
                                 "id"             integer NOT NULL,
                                 "Time"           timestamp,
                                 "depth"          double,
                                 "temperature"    double);

INSERT INTO "mesures"."mesure5" VALUES (507, 1, '2007-05-01 12:59:00',6.56, NULL);
INSERT INTO "mesures"."mesure5" VALUES (507, 2, '2007-05-01 13:59:00',6.56, NULL);
INSERT INTO "mesures"."mesure5" VALUES (507, 3, '2007-05-01 14:59:00',6.56, NULL);
INSERT INTO "mesures"."mesure5" VALUES (507, 4, '2007-05-01 15:59:00',6.56, NULL);
INSERT INTO "mesures"."mesure5" VALUES (507, 5, '2007-05-01 16:59:00',6.56, NULL);

CREATE TABLE "mesures"."mesure4"("id_observation" integer NOT NULL,
                                 "id"             integer NOT NULL,
                                 "Time"           timestamp,
                                 "depth"          double);

INSERT INTO "mesures"."mesure4" VALUES (406, 1, '2007-05-01 12:59:00',6.56);
INSERT INTO "mesures"."mesure4" VALUES (406, 2, '2007-05-01 13:59:00',6.56);
INSERT INTO "mesures"."mesure4" VALUES (406, 3, '2007-05-01 14:59:00',6.56);
INSERT INTO "mesures"."mesure4" VALUES (406, 4, '2007-05-01 15:59:00',6.56);
INSERT INTO "mesures"."mesure4" VALUES (406, 5, '2007-05-01 16:59:00',6.56);

CREATE TABLE "mesures"."mesure8"("id_observation" integer NOT NULL,
                                 "id"             integer NOT NULL,
                                 "Time"           timestamp,
                                 "depth"          double,
                                 "temperature"    double);

INSERT INTO "mesures"."mesure8" VALUES (801, 1,  '2007-05-01 12:59:00',6.56,12.0);
INSERT INTO "mesures"."mesure8" VALUES (801, 3,  '2007-05-01 13:59:00',6.56,13.0);
INSERT INTO "mesures"."mesure8" VALUES (801, 5,  '2007-05-01 14:59:00',6.56,14.0);
INSERT INTO "mesures"."mesure8" VALUES (801, 7,  '2007-05-01 15:59:00',6.56,15.0);
INSERT INTO "mesures"."mesure8" VALUES (801, 9,  '2007-05-01 16:59:00',6.56,16.0);

CREATE TABLE "mesures"."mesure7"("id_observation" integer NOT NULL,
                                 "id"             integer NOT NULL,
                                 "Time"           timestamp,
                                 "temperature"    double);

INSERT INTO "mesures"."mesure7" VALUES (702, 1,  '2007-05-01 16:59:00',6.56);

CREATE TABLE "mesures"."mesure9"("id_observation" integer NOT NULL,
                                 "id"             integer NOT NULL,
                                 "depth"          double);

INSERT INTO "mesures"."mesure9" VALUES (901, 1,  15.5);
INSERT INTO "mesures"."mesure9" VALUES (901, 2,  19.7);
INSERT INTO "mesures"."mesure9" VALUES (901, 3,  21.2);
INSERT INTO "mesures"."mesure9" VALUES (901, 4,  23.9);
INSERT INTO "mesures"."mesure9" VALUES (901, 5,  22.2);
INSERT INTO "mesures"."mesure9" VALUES (901, 6,  18.4);
INSERT INTO "mesures"."mesure9" VALUES (901, 7,  17.1);

CREATE TABLE "mesures"."mesure10"("id_observation" integer NOT NULL,
                                 "id"             integer NOT NULL,
                                 "Time"           timestamp,
                                 "depth"          double);

INSERT INTO "mesures"."mesure10" VALUES (1001, 1,  '2009-05-01 13:47:00',4.5);
INSERT INTO "mesures"."mesure10" VALUES (1001, 2,  '2009-05-01 14:00:00',5.9);
INSERT INTO "mesures"."mesure10" VALUES (1002, 1,  '2009-05-01 14:01:00',8.9);
INSERT INTO "mesures"."mesure10" VALUES (1002, 2,  '2009-05-01 14:02:00',7.8);
INSERT INTO "mesures"."mesure10" VALUES (1002, 3,  '2009-05-01 14:03:00',9.9);
INSERT INTO "mesures"."mesure10" VALUES (1003, 1,  '2009-05-01 14:04:00',9.1);

CREATE TABLE "mesures"."mesure11"("id_observation" integer NOT NULL,
                                  "id"             integer NOT NULL,
                                  "Time"           timestamp,
                                  "depth"          double);

INSERT INTO "mesures"."mesure11" VALUES (2000, 1,  '2009-05-01 13:47:00',4.5);
INSERT INTO "mesures"."mesure11" VALUES (2000, 2,  '2009-05-01 14:00:00',5.9);
INSERT INTO "mesures"."mesure11" VALUES (2000, 3,  '2009-05-01 14:01:00',8.9);
INSERT INTO "mesures"."mesure11" VALUES (2000, 4,  '2009-05-01 14:02:00',7.8);
INSERT INTO "mesures"."mesure11" VALUES (2000, 5,  '2009-05-01 14:03:00',9.9);

CREATE TABLE "mesures"."mesure12"("id_observation" integer NOT NULL,
                                  "id"             integer NOT NULL,
                                  "Time"           timestamp,
                                  "depth"          double,
                                  "temperature"    double);

INSERT INTO "mesures"."mesure12" VALUES (3000, 1,  '2000-12-01 00:00:00',2.5, 98.5);
INSERT INTO "mesures"."mesure12" VALUES (3000, 2,  '2009-12-01 14:00:00',5.9,  1.5);
INSERT INTO "mesures"."mesure12" VALUES (3000, 3,  '2009-12-11 14:01:00',8.9, 78.5);
INSERT INTO "mesures"."mesure12" VALUES (3000, 4,  '2009-12-15 14:02:00',7.8, 14.5);
INSERT INTO "mesures"."mesure12" VALUES (3000, 5,  '2012-12-22 00:00:00',9.9,  5.5);

CREATE TABLE "mesures"."mesure12_2"("id_observation" integer NOT NULL,
                                    "id"             integer NOT NULL,
                                    "salinity"       double);

INSERT INTO "mesures"."mesure12_2" VALUES (3000, 1,  4);
INSERT INTO "mesures"."mesure12_2" VALUES (3000, 2,  3);
INSERT INTO "mesures"."mesure12_2" VALUES (3000, 3,  2);
INSERT INTO "mesures"."mesure12_2" VALUES (3000, 4,  1);
INSERT INTO "mesures"."mesure12_2" VALUES (3000, 5,  0);

CREATE TABLE "mesures"."mesure13"("id_observation" integer NOT NULL,
                                  "id"             integer NOT NULL,
                                  "Time"           timestamp,
                                  "depth"          double,
                                  "temperature"    double,
                                  "salinity"       double);

-- inserted in time disorder on purpose
INSERT INTO "mesures"."mesure13" VALUES (4000, 1,  '2000-05-01 00:00:00', 4.9, NULL, NULL);
INSERT INTO "mesures"."mesure13" VALUES (4000, 2,  '2000-06-01 00:00:00', 5.0, NULL, NULL);
INSERT INTO "mesures"."mesure13" VALUES (4000, 3,  '2000-07-01 00:00:00', 5.1, NULL, NULL);

INSERT INTO "mesures"."mesure13" VALUES (4001, 1,  '2000-01-01 00:00:00', 4.5, 98.5, NULL);
INSERT INTO "mesures"."mesure13" VALUES (4001, 2,  '2000-02-01 00:00:00', 4.6, 97.5, NULL);
INSERT INTO "mesures"."mesure13" VALUES (4001, 3,  '2000-03-01 00:00:00', 4.7, 97.5, NULL);
INSERT INTO "mesures"."mesure13" VALUES (4001, 4,  '2000-04-01 00:00:00', 4.8, 96.5, NULL);

INSERT INTO "mesures"."mesure13" VALUES (4002, 1,  '2000-08-01 00:00:00', 5.2, 98.5, 1.1);
INSERT INTO "mesures"."mesure13" VALUES (4002, 2,  '2000-09-01 00:00:00', 5.3, 87.5, 1.1);
INSERT INTO "mesures"."mesure13" VALUES (4002, 3,  '2000-10-01 00:00:00', 5.4, 77.5, 1.3);

INSERT INTO "mesures"."mesure13" VALUES (4003, 1,  '2000-11-01 00:00:00', NULL, 96.5, NULL);
INSERT INTO "mesures"."mesure13" VALUES (4003, 2,  '2000-12-01 00:00:00', NULL, 99.5, NULL);
INSERT INTO "mesures"."mesure13" VALUES (4003, 3,  '2001-01-01 00:00:00', NULL, 96.5, NULL);

CREATE TABLE "mesures"."mesure14"("id_observation" integer NOT NULL,
                                 "id"             integer NOT NULL,
                                 "depth"          double,
                                 "temperature"    double,
                                 "salinity"       double);

INSERT INTO "mesures"."mesure14" VALUES (5001, 1,  18.5, 12.8, NULL);
INSERT INTO "mesures"."mesure14" VALUES (5001, 2,  19.7, 12.7, NULL);
INSERT INTO "mesures"."mesure14" VALUES (5001, 3,  21.2, 12.6, NULL);
INSERT INTO "mesures"."mesure14" VALUES (5001, 4,  23.9, 12.5, NULL);
INSERT INTO "mesures"."mesure14" VALUES (5001, 5,  24.2, 12.4, NULL);
INSERT INTO "mesures"."mesure14" VALUES (5001, 6,  29.4, 12.3, NULL);
INSERT INTO "mesures"."mesure14" VALUES (5001, 7,  31.1, 12.2, NULL);

INSERT INTO "mesures"."mesure14" VALUES (5002, 1,  18.5, 12.8, NULL);
INSERT INTO "mesures"."mesure14" VALUES (5002, 2,  19.7, 12.9, NULL);
INSERT INTO "mesures"."mesure14" VALUES (5002, 3,  21.2, 13.0, NULL);
INSERT INTO "mesures"."mesure14" VALUES (5002, 4,  23.9, 13.1, NULL);
INSERT INTO "mesures"."mesure14" VALUES (5002, 5,  24.2, 13.2, NULL);
INSERT INTO "mesures"."mesure14" VALUES (5002, 6,  29.4, 13.3, NULL);
INSERT INTO "mesures"."mesure14" VALUES (5002, 7,  31.1, 13.4, NULL);

INSERT INTO "mesures"."mesure14" VALUES (5003, 1,  18.5, 12.8, 5.1);
INSERT INTO "mesures"."mesure14" VALUES (5003, 2,  19.7, 12.7, 5.2);
INSERT INTO "mesures"."mesure14" VALUES (5003, 3,  21.2, 12.6, 5.3);
INSERT INTO "mesures"."mesure14" VALUES (5003, 4,  23.9, 12.5, 5.4);
INSERT INTO "mesures"."mesure14" VALUES (5003, 5,  24.2, 12.4, 5.5);
INSERT INTO "mesures"."mesure14" VALUES (5003, 6,  29.4, 12.3, 5.6);
INSERT INTO "mesures"."mesure14" VALUES (5003, 7,  31.1, 12.2, 5.7);

INSERT INTO "mesures"."mesure14" VALUES (5004, 1,  18.5, 12.8, 5.1);
INSERT INTO "mesures"."mesure14" VALUES (5004, 2,  19.7, 12.9, 5.0);
INSERT INTO "mesures"."mesure14" VALUES (5004, 3,  21.2, 13.0, 4.9);
INSERT INTO "mesures"."mesure14" VALUES (5004, 4,  23.9, 13.1, 4.8);
INSERT INTO "mesures"."mesure14" VALUES (5004, 5,  24.2, 13.2, 4.7);
INSERT INTO "mesures"."mesure14" VALUES (5004, 6,  29.4, 13.3, 4.6);
INSERT INTO "mesures"."mesure14" VALUES (5004, 7,  31.1, 13.4, 4.5);

CREATE TABLE "mesures"."mesure15"("id_observation"      integer NOT NULL,
                                  "id"                  integer NOT NULL,
                                  "Time"                timestamp NOT NULL,
                                  "depth"               double,
                                  "depth_quality_qflag" character varying(1000),
                                  "depth_quality_qres"  double); 

INSERT INTO "mesures"."mesure15" VALUES (6001, 1,  '1980-03-01 21:52:00', 6.56, 'ok', 3.1);
INSERT INTO "mesures"."mesure15" VALUES (6001, 2,  '1981-03-01 21:52:00', 6.56, 'ko', 3.2);
INSERT INTO "mesures"."mesure15" VALUES (6001, 3,  '1982-03-01 21:52:00', 6.56, 'ok', 3.3);
INSERT INTO "mesures"."mesure15" VALUES (6001, 4,  '1983-03-01 21:52:00', 6.56, 'ko', 3.4);
INSERT INTO "mesures"."mesure15" VALUES (6001, 5,  '1984-03-01 21:52:00', 6.56, 'ok', 3.5);

CREATE TABLE "mesures"."mesure16"("id_observation"      integer NOT NULL,
                                  "id"                  integer NOT NULL,
                                  "Time"                timestamp NOT NULL,
                                  "isHot"               integer,
                                  "color"               character varying(10000),
                                  "expiration"          timestamp,
                                  "age"                 double); 

INSERT INTO "mesures"."mesure16" VALUES (7001, 1,  '1980-03-01 21:52:00', 0, 'blue', '2000-01-01 22:10:00', 12);
INSERT INTO "mesures"."mesure16" VALUES (7001, 2,  '1981-03-01 22:52:00', 1, 'red',  '2001-01-01 22:10:00', 14);

CREATE TABLE "mesures"."mesure17"("id_observation"             integer NOT NULL,
                                  "id"                        integer NOT NULL,
                                  "depth"                     double,
                                  "isHot"                     integer,
                                  "isHot_quality_isHot_qual"  integer); 

INSERT INTO "mesures"."mesure17" VALUES (8001, 1,  1.0, 0, 0);
INSERT INTO "mesures"."mesure17" VALUES (8001, 2,  2.0, 1, 0);
INSERT INTO "mesures"."mesure17" VALUES (8001, 3,  3.0, 0, 1);

INSERT INTO "mesures"."mesure17" VALUES (8002, 1,  1.0, 1, 1);
INSERT INTO "mesures"."mesure17" VALUES (8002, 2,  2.0, 1, 1);
INSERT INTO "mesures"."mesure17" VALUES (8002, 3,  3.0, 1, 1);

INSERT INTO "mesures"."mesure17" VALUES (8003, 1,  1.0, 0, 0);
INSERT INTO "mesures"."mesure17" VALUES (8003, 2,  2.0, 0, 0);
INSERT INTO "mesures"."mesure17" VALUES (8003, 3,  3.0, 0, 0);

CREATE TABLE "mesures"."mesure17_2"("id_observation"                     integer NOT NULL,
                                    "id"                                 integer NOT NULL,
                                    "color"                              character varying(10000),
                                    "color_quality_color_qual"           character varying(10000),
                                    "expiration"                         timestamp,
                                    "expiration_quality_expiration_qual"  timestamp); 

INSERT INTO "mesures"."mesure17_2" VALUES (8001, 1, 'blue',  'good', '2000-01-01 22:00:00', '2000-01-01 23:00:00');
INSERT INTO "mesures"."mesure17_2" VALUES (8001, 2, 'green', 'fade', '2000-01-01 22:00:00', '2000-01-01 23:00:00');
INSERT INTO "mesures"."mesure17_2" VALUES (8001, 3, 'red',   'bad',  '2000-01-01 22:00:00', '2000-01-01 23:00:00');

INSERT INTO "mesures"."mesure17_2" VALUES (8002, 1, 'yellow',  'good', '2000-01-02 22:00:00', '2000-01-02 23:00:00');
INSERT INTO "mesures"."mesure17_2" VALUES (8002, 2, 'yellow',  'good', '2000-01-02 22:00:00', '2000-01-02 23:00:00');
INSERT INTO "mesures"."mesure17_2" VALUES (8002, 3, 'yellow',  'good', '2000-01-02 22:00:00', '2000-01-02 23:00:00');

INSERT INTO "mesures"."mesure17_2" VALUES (8003, 1, 'brown',  'bad',  '2000-01-03 22:00:00', '2000-01-03 23:00:00');
INSERT INTO "mesures"."mesure17_2" VALUES (8003, 2, 'black',  'fade', '2000-01-03 22:00:00', '2000-01-03 23:00:00');
INSERT INTO "mesures"."mesure17_2" VALUES (8003, 3, 'black',  'fade', '2000-01-03 22:00:00', '2000-01-03 23:00:00');

CREATE TABLE "mesures"."mesure17_3"("id_observation"      integer NOT NULL,
                                    "id"                  integer NOT NULL,
                                    "age"                 double,
                                    "age_quality_age_qual" double); 

INSERT INTO "mesures"."mesure17_3" VALUES (8001, 1,  27.0, 37.0);
INSERT INTO "mesures"."mesure17_3" VALUES (8001, 2,  28.0, 38.0);
INSERT INTO "mesures"."mesure17_3" VALUES (8001, 3,  29.0, 39.1);

INSERT INTO "mesures"."mesure17_3" VALUES (8002, 1,  16.3, 16.3);
INSERT INTO "mesures"."mesure17_3" VALUES (8002, 2,  26.4, 25.4);
INSERT INTO "mesures"."mesure17_3" VALUES (8002, 3,  30.0, 28.1);

INSERT INTO "mesures"."mesure17_3" VALUES (8003, 1,  11.0, 0.0);
INSERT INTO "mesures"."mesure17_3" VALUES (8003, 2,  22.0, 0.0);
INSERT INTO "mesures"."mesure17_3" VALUES (8003, 3,  33.0, 0.0);
