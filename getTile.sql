-- example functions for PostGIS backend of VectorTileMap
-- TODO make default work with standard psm2psql database 

CREATE OR REPLACE FUNCTION __get_tile(IN tilex bigint, IN tiley bigint, IN tilez integer)
RETURNS TABLE(id bigint, tags hstore, geom bytea) AS
$BODY$
DECLARE
bbox geometry;
min float;
BEGIN
	bbox := __tileToBox3D(tileX, tileY, tileZ);
	-- pixel size at zoomlevel
	min := 20037508.342789244 / 256 / (2 ^ tileZ);
	
	IF (tileZ > 14) THEN
		RETURN QUERY SELECT * FROM __get_tile15(bbox, min)
			     UNION ALL
			     SELECT * FROM __get_tile7(bbox, min);
	ELSIF (tileZ > 13) THEN
		RETURN QUERY SELECT * FROM __get_tile14(bbox)
			     UNION ALL
			     SELECT * FROM __get_tile7(bbox, min);
	ELSIF (tileZ > 11) THEN
		RETURN QUERY SELECT * FROM __get_tile12(bbox, min) 
			     UNION ALL
			     SELECT * FROM __get_tile7(bbox, min);
	ELSIF (tileZ > 8) THEN
		RETURN QUERY SELECT * FROM __get_tile7(bbox, min)
			     UNION ALL
			     SELECT * FROM __get_tile_simp_ways2(bbox, min)
			     UNION ALL
			     SELECT * FROM __get_tile_admin_0(bbox, min);
	ELSIF (tileZ > 6) THEN
		RETURN QUERY SELECT * FROM __get_tile4(bbox, min)
			     UNION ALL
			     SELECT * FROM __get_tile_simp_ways3(bbox, min)
			     UNION ALL
			     SELECT * FROM __get_tile_admin_0(bbox, min);
	ELSIF (tileZ > 3) THEN
		RETURN QUERY SELECT * FROM __get_tile3(bbox, min)
			     UNION ALL
			     SELECT * FROM __get_tile_admin_0(bbox, min);
	ELSE
		RETURN QUERY SELECT * FROM __get_tile0(bbox, min)
			     UNION ALL
			     SELECT * FROM __get_tile_admin_0(bbox, min);
	END IF;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE;


CREATE OR REPLACE FUNCTION __tiletobox3d(tilex bigint, tiley bigint, tilez integer)
  RETURNS geometry AS
$BODY$
DECLARE
scaleFactor double precision;
minLon double precision;
maxLon double precision;
minLat double precision;
maxLat double precision;
tileSize bigint;
center double precision;
pixel integer;
BEGIN
	pixel := 10;
	scaleFactor := 20037508.342789244;
	tileSize := 256;
	center := (tileSize << tileZ) >> 1;
		
	minLat := ((center - (tileY + tileSize + pixel)) / center) * scaleFactor;
	maxLat := ((center - (tileY - pixel)) / center) * scaleFactor;

	minLon := (((tileX - pixel) - center) / center) * scaleFactor;
	maxLon := (((tileX + tileSize + pixel) - center) / center) * scaleFactor;
	
	if (minLat < -scaleFactor) then
		minLat := -scaleFactor;
	elsif (minLat > scaleFactor) then
		minLat := scaleFactor;
	end if;
	
	if (minLon < -scaleFactor) then
		minLon := -scaleFactor;
	elsif (minLon > scaleFactor) then
		minLon := scaleFactor;
	end if;
	
	if (maxLat < -scaleFactor) then
		maxLat := -scaleFactor;
	elsif (maxLat > scaleFactor) then
		maxLat := scaleFactor;
	end if;
		
	if (maxLon < -scaleFactor) then
		maxLon := -scaleFactor;
	elsif (maxLon > scaleFactor) then
		maxLon := scaleFactor;
	end if;
	-- RAISE NOTICE 'c %, %, %, %,%', center, minLon, minLat, maxLon, maxLat;	
	RETURN ST_SetSRID(concat('BOX3D(',minLon,' ', minLat, ',',  maxLon, ' ', maxLat, ')')::box3d , 900913);
END;
$BODY$
  LANGUAGE plpgsql VOLATILE;
  
CREATE OR REPLACE FUNCTION __get_tile0(IN bbox geometry, IN pixel float)
RETURNS TABLE(id bigint, tags hstore, geom bytea) AS
$BODY$
SELECT 1::bigint as id, ('natural' => 'water') as tags,
	ST_AsEWKB(ST_Collect(ST_SimplifyPreserveTopology(geom, $2))) FROM 
	(SELECT (ST_Dump(ST_Intersection(geomp, $1))).geom AS geom FROM
	ne_110m_ocean
	WHERE geomp && $1) p WHERE geometrytype(geom) = 'POLYGON';
 $BODY$
  LANGUAGE sql VOLATILE;



CREATE OR REPLACE FUNCTION __get_tile3(IN bbox geometry, IN pixel float)
RETURNS TABLE(id bigint, tags hstore, geom bytea) AS
$BODY$
SELECT 1::bigint as id, ('natural' => 'water') as tags,
	ST_AsEWKB(ST_Collect(ST_SimplifyPreserveTopology(geom, 2*$2))) FROM 
	(SELECT (ST_Dump(ST_Intersection(geomp, $1))).geom AS geom FROM
	ne_50m_ocean
	WHERE geomp && $1) p WHERE geometrytype(geom) = 'POLYGON';
 $BODY$
  LANGUAGE sql VOLATILE;


 
CREATE OR REPLACE FUNCTION __get_tile4(IN bbox geometry, IN pixel float)
  RETURNS TABLE(id bigint, tags hstore, geom bytea) AS
$BODY$
SELECT 1::bigint as id, ('natural' => 'water') as tags,
	COALESCE (ST_AsEWKB(ST_SymDifference(ST_Buffer(ST_Collect(ST_SimplifyPreserveTopology(geom, 2*$2)),-5*$2,0), $1)), ST_AsEWKB($1)) FROM 
	(SELECT (ST_Dump(ST_Buffer(ST_Intersection($1, st_buffer(the_geom,0)),5*$2,0))).geom AS geom FROM
	shorelines
	WHERE the_geom && $1) p WHERE geometrytype(geom) = 'POLYGON' AND ST_Area(geom) > (500*$2^2);
 $BODY$
  LANGUAGE sql VOLATILE;


  
CREATE OR REPLACE FUNCTION __get_tile7(IN bbox geometry, IN pixel float)
  RETURNS TABLE(id bigint, tags hstore, geom bytea) AS
$BODY$
SELECT 1::bigint as id, ('natural' => 'water') as tags,
	COALESCE (ST_AsEWKB(ST_SymDifference(ST_Buffer(ST_Collect(ST_SimplifyPreserveTopology(geom, 2*$2)), -5*$2,0), $1)), ST_AsEWKB($1)) FROM 
	(SELECT (ST_Dump(ST_Buffer(ST_Intersection($1, the_geom),5*$2,0))).geom AS geom FROM
	coastlines
	WHERE the_geom && $1) p WHERE geometrytype(geom) = 'POLYGON' AND ST_Area(geom) > (500*$2^2);
 $BODY$
  LANGUAGE sql VOLATILE;



CREATE OR REPLACE FUNCTION __get_tile_simp_ways3(IN bbox geometry, IN pixel float)
  RETURNS TABLE(id bigint, tags hstore, geom bytea) AS
$BODY$
SELECT 1::bigint as id, ('highway' => way_type) as tags,
	ST_AsEWKB(ST_SimplifyPreserveTopology(geom, 2*$2)) FROM 
	(SELECT (ST_Dump(ST_Intersection($1, geom))).geom AS geom, way_type FROM
	ways_simp
	WHERE geom && $1) p WHERE geometrytype(geom) in ('LINESTRING');
 $BODY$
  LANGUAGE sql VOLATILE;


CREATE OR REPLACE FUNCTION __get_tile_simp_ways(IN bbox geometry, IN pixel float)
  RETURNS TABLE(id bigint, tags hstore, geom bytea) AS
$BODY$
SELECT 1::bigint as id, ('highway' => way_type) as tags,
	ST_AsEWKB(ST_SimplifyPreserveTopology(geom, 2*$2)) FROM 
	(SELECT (ST_Dump(ST_Intersection($1, the_geom))).geom AS geom, way_type FROM
	simple_ways2
	WHERE the_geom && $1 AND way_type in ('motorway', 'motorrway_link', 'primary', 'primary_link', 'trunk', 'trunk_link')) p WHERE geometrytype(geom) in ('LINESTRING');
 $BODY$
  LANGUAGE sql VOLATILE;


  CREATE OR REPLACE FUNCTION __get_tile_simp_ways2(IN bbox geometry, IN pixel float)
  RETURNS TABLE(id bigint, tags hstore, geom bytea) AS
$BODY$
SELECT 1::bigint as id, ('highway' => way_type) as tags,
	ST_AsEWKB(ST_SimplifyPreserveTopology(geom, 2*$2)) FROM 
	(SELECT (ST_Dump(ST_Intersection($1, the_geom))).geom AS geom, way_type FROM
	simple_ways2
	WHERE the_geom && $1) p WHERE geometrytype(geom) in ('LINESTRING');
 $BODY$
  LANGUAGE sql VOLATILE;



  CREATE OR REPLACE FUNCTION __get_tile_admin_0(IN bbox geometry, IN pixel float)
  RETURNS TABLE(id bigint, tags hstore, geom bytea) AS
$BODY$
SELECT 1::bigint as id, ('admin_level' => '2') as tags,
	ST_AsEWKB(ST_SimplifyPreserveTopology(geom, 2*$2)) FROM 
	(SELECT (ST_Dump(ST_Intersection($1, geom))).geom AS geom FROM
	ne_50m_admin_0_boundary_lines_land
	WHERE geom && $1) p;
 $BODY$
  LANGUAGE sql VOLATILE;




CREATE OR REPLACE FUNCTION __get_tile12(IN bbox geometry, IN pixel float)
  RETURNS TABLE(id bigint, tags hstore, geom bytea) AS
$BODY$
WITH polygons AS 
(SELECT way, 
	CASE  WHEN (landuse is not null) THEN ('landuse' => landuse)
	WHEN (waterway is not null)  THEN ('waterway' => waterway)
	-- WHEN (building is not null)  THEN ('building' => building) 
	 WHEN ("natural" is not null) THEN ('natural' => "natural")
	 WHEN  (leisure is not null) THEN ('leisure' => leisure) 
	END AS tags
 FROM planet_polygon 
	WHERE way && $1 
	AND ((landuse is not null)
		OR (waterway is not null) 
		--OR (building is not null) 
		OR ("natural" is not null) 
		OR (leisure is not null))
	AND way_area > 400 * ($2^2))
		
SELECT 1::bigint AS id, ('highway' => highway)::hstore, ST_AsEWKB((ST_Dump(ST_Multi(ST_SimplifyPreserveTopology(ST_LineMerge(way), $2)))).geom) AS geom FROM
 (SELECT ST_Intersection(ST_Collect(way), $1) as way, highway FROM
  planet_line WHERE 
  (highway IS NOT NULL) AND 
 way && $1 AND (highway in ('motorway', 'motorway_link', 'trunk', 'trunk_link', 'primary', 'primary_link', 'secondary', 'secondary_link', 'tertiary'))
 group by highway) p
  UNION ALL 
SELECT 1::bigint AS id, tags, ST_AsEWKB(ST_Collect(geom)) AS geom FROM
(SELECT tags, (ST_Dump(ST_Buffer(ST_SimplifyPreserveTopology(way, 2*$2), - $2*4,0))).geom as geom FROM 
-- (SELECT ST_Intersection(ST_Union(ST_SimplifyPreserveTopology(ST_Buffer(way,$2,0), 2*$2)), $1) as way, tags FROM
(SELECT ST_Intersection(ST_Union(ST_Buffer(way,$2*4,0)), $1) as way, tags FROM
  polygons group by tags ) p ) p WHERE geometrytype(geom) = 'POLYGON' group by tags
 $BODY$
  LANGUAGE sql VOLATILE;



CREATE OR REPLACE FUNCTION __get_tile14(IN bbox geometry, IN pixel float)
RETURNS TABLE(id bigint, tags hstore, geom bytea) AS
$BODY$
WITH polygons AS 
(SELECT way, 
	CASE  WHEN (landuse is not null) THEN ('landuse' => landuse)
	WHEN (waterway is not null)  THEN ('waterway' => waterway)
	-- WHEN (building is not null)  THEN ('building' => building) 
	 WHEN ("natural" is not null) THEN ('natural' => "natural")
	 WHEN  (leisure is not null) THEN ('leisure' => leisure) 
	END AS tags
 FROM planet_polygon 
	WHERE way && $1 
	AND ((landuse is not null)
		OR (waterway is not null) 
		--OR (building is not null) 
		OR ("natural" is not null) 
		OR leisure is not null))
		
SELECT 1::bigint AS id, ('highway' => highway)::hstore, ST_AsEWKB((ST_Dump(ST_Multi(ST_SimplifyPreserveTopology(ST_LineMerge(way), $2)))).geom) AS geom FROM
 (SELECT ST_Intersection(ST_Union(way), $1) as way, highway FROM
  planet_line WHERE 
  (highway IS NOT NULL) AND 
 way && $1 AND (highway in ('motorway', 'motorway_link', 'trunk', 'trunk_link', 'primary', 'primary_link', 'secondary', 'secondary_link', 'tertiary', 'residential'))
 group by highway) p
  UNION ALL 

SELECT 1::bigint AS id, tags, ST_AsEWKB(ST_Collect(geom)) AS geom FROM
(SELECT tags, (ST_Dump(way)).geom as geom FROM 
 (SELECT ST_Intersection(ST_Union(ST_Buffer(way,0)), $1) as way, tags FROM
  polygons group by tags ) p ) p WHERE geometrytype(geom) = 'POLYGON' group by tags
 $BODY$
  LANGUAGE sql VOLATILE;
  


CREATE OR REPLACE FUNCTION __get_tile15(IN bbox geometry, IN pixel float)
RETURNS TABLE(id bigint, tags hstore, geom bytea) AS
$BODY$
WITH polygons AS 
(SELECT way, 
	CASE  WHEN (landuse is not null) THEN ('landuse' => landuse)
	WHEN (waterway is not null)  THEN ('waterway' => waterway)
	 WHEN (building is not null)  THEN ('building' => building) 
	 WHEN ("natural" is not null) THEN ('natural' => "natural")
	 WHEN  (leisure is not null) THEN ('leisure' => leisure) 
	END AS tags
 FROM planet_polygon 
	WHERE way && $1 
	AND ((landuse is not null)
		OR (waterway is not null) 
		OR (building is not null) 
		OR ("natural" is not null) 
		OR leisure is not null))
		
SELECT 1::bigint AS id, ('highway' => highway)::hstore, ST_AsEWKB((ST_Dump(ST_Multi(ST_SimplifyPreserveTopology(ST_LineMerge(way), $2)))).geom) AS geom FROM
 (SELECT ST_Intersection(ST_Union(way), $1) as way, highway FROM
  planet_line WHERE 
  (highway IS NOT NULL) AND 
 way && $1
 group by highway) p
  UNION ALL 

SELECT 1::bigint AS id, tags, ST_AsEWKB(ST_Collect(geom)) AS geom FROM
(SELECT tags, (ST_Dump(way)).geom as geom FROM 
 (SELECT ST_Intersection(ST_Union(ST_Buffer(way,$2*10,0)), $1) as way, tags FROM
  polygons group by tags ) p ) p WHERE geometrytype(geom) = 'POLYGON' group by tags
 $BODY$
  LANGUAGE sql VOLATILE;