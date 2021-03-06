/**
 * This class is generated by jOOQ
 */
package org.constellation.database.api.jooq.tables.pojos;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.5.3"
	},
	comments = "This class is generated by jOOQ"
)
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetadataBbox implements java.io.Serializable {

	private static final long serialVersionUID = -1038257296;

	private java.lang.Integer metadataId;
	private java.lang.Double  east;
	private java.lang.Double  west;
	private java.lang.Double  north;
	private java.lang.Double  south;

	public MetadataBbox() {}

	public MetadataBbox(
		java.lang.Integer metadataId,
		java.lang.Double  east,
		java.lang.Double  west,
		java.lang.Double  north,
		java.lang.Double  south
	) {
		this.metadataId = metadataId;
		this.east = east;
		this.west = west;
		this.north = north;
		this.south = south;
	}

	@javax.validation.constraints.NotNull
	public java.lang.Integer getMetadataId() {
		return this.metadataId;
	}

	public MetadataBbox setMetadataId(java.lang.Integer metadataId) {
		this.metadataId = metadataId;
		return this;
	}

	@javax.validation.constraints.NotNull
	public java.lang.Double getEast() {
		return this.east;
	}

	public MetadataBbox setEast(java.lang.Double east) {
		this.east = east;
		return this;
	}

	@javax.validation.constraints.NotNull
	public java.lang.Double getWest() {
		return this.west;
	}

	public MetadataBbox setWest(java.lang.Double west) {
		this.west = west;
		return this;
	}

	@javax.validation.constraints.NotNull
	public java.lang.Double getNorth() {
		return this.north;
	}

	public MetadataBbox setNorth(java.lang.Double north) {
		this.north = north;
		return this;
	}

	@javax.validation.constraints.NotNull
	public java.lang.Double getSouth() {
		return this.south;
	}

	public MetadataBbox setSouth(java.lang.Double south) {
		this.south = south;
		return this;
	}
}
