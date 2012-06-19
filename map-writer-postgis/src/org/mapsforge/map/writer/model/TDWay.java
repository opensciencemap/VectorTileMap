/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.map.writer.model;

import java.util.Arrays;

import org.mapsforge.map.writer.util.GeoUtils;
import org.mapsforge.map.writer.util.OSMUtils;

import com.vividsolutions.jts.geom.Geometry;

import de.sfb.tilemap.writer.util.PGHStore;

/**
 * Represents an OSM way.
 * 
 * @author bross
 */
public class TDWay {

	//   private static final Logger LOGGER = Logger.getLogger(TDWay.class.getName());

	// TODO these constants are not necessary anymore
	/**
	 * Represents a line.
	 */
	public static final byte LINE = 0x0;
	/**
	 * A simple closed polygon.
	 */
	public static final byte SIMPLE_POLYGON = 0x1;
	/**
	 * A simple closed polygon with holes.
	 */
	public static final byte MULTI_POLYGON = 0x2;

	private final long id;
	private final byte layer;
	private String name;
	private String ref;
	private final String houseNumber;
	private short[] tags; // NOPMD by bross on 25.12.11 13:04
	private byte shape;
	private final TDNode[] wayNodes;
	private boolean reversedInRelation;
	private boolean invalid;
	private Geometry geometry;

	/**
	 * Creates a new TDWay from an osmosis way entity using the given NodeResolver.
	 * 
	 * @param way
	 *            the way
	 * @param resolver
	 *            the resolver
	 * @param preferredLanguage
	 *            the preferred language or null if no preference
	 * @return a new TDWay if it is valid, null otherwise
	 */
	public static TDWay fromWay(long id, PGHStore tags, Geometry geom, String preferredLanguage) {

		SpecialTagExtractionResult ster = OSMUtils.extractSpecialFields(tags, preferredLanguage);
		short[] knownWayTags = OSMUtils.extractKnownWayTags(tags);

		// // only ways with at least 2 way nodes are valid ways
		// if (way.getWayNodes().size() >= 2) {
		//
		// boolean validWay = true;
		// // retrieve way nodes from data store
		// TDNode[] waynodes = new TDNode[way.getWayNodes().size()];
		// int i = 0;
		// for (WayNode waynode : way.getWayNodes()) {
		// // TODO adjust interface to support a method getWayNodes()
		// waynodes[i] = resolver.getNode(waynode.getNodeId());
		// if (waynodes[i] == null) {
		// validWay = false;
		// LOGGER.finer("unknown way node: " + waynode.getNodeId() + " in way " + way.getId());
		// }
		// i++;
		// }
		//
		// // for a valid way all way nodes must be existent in the input data
		// if (validWay) {
		//
		// // mark the way as polygon if the first and the last way node are the same
		// // and if the way has at least 4 way nodes
		// 
		// if (waynodes[0].getId() == waynodes[waynodes.length - 1].getId()) {
		// if (waynodes.length >= GeoUtils.MIN_NODES_POLYGON) {
		// shape = SIMPLE_POLYGON;
		// } else {
		// LOGGER.finer("Found closed polygon with fewer than 4 way nodes. Way-id: " + way.getId());
		// return null;
		// }
		// }

		java.util.Arrays.sort(knownWayTags);

		byte shape = LINE;
		//if ("LINESTRING".equals(geom.getGeometryType() == null)
		
		return new TDWay(id, ster.getLayer(), ster.getName(), ster.getHousenumber(), ster.getRef(),
				knownWayTags, shape, geom);
		// }
		// }

		//return null;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            the id
	 * @param layer
	 *            the layer
	 * @param name
	 *            the name if existent
	 * @param houseNumber
	 *            the house number if existent
	 * @param ref
	 *            the ref if existent
	 * @param wayNodes
	 *            the way nodes
	 */
	public TDWay(long id, byte layer, String name, String houseNumber, String ref, TDNode[] wayNodes) {
		this.id = id;
		this.layer = layer;
		this.name = name;
		this.houseNumber = houseNumber;
		this.ref = ref;
		this.wayNodes = wayNodes;
	}

	public TDWay(long id, byte layer, String name, String houseNumber, String ref, short[] tags, byte shape, Geometry geom) {
		this.id = id;
		this.layer = layer;
		this.name = name;
		this.houseNumber = houseNumber;
		this.ref = ref;
		this.tags = tags;
		this.shape = shape;
		this.setGeometry(geom);
		this.wayNodes = null;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            the id
	 * @param layer
	 *            the layer
	 * @param name
	 *            the name if existent
	 * @param houseNumber
	 *            the house number if existent
	 * @param ref
	 *            the ref if existent
	 * @param tags
	 *            the tags
	 * @param shape
	 *            the shape
	 * @param wayNodes
	 *            the way nodes
	 */
	public TDWay(long id, byte layer, String name, String houseNumber, String ref, short[] tags, byte shape,
			TDNode[] wayNodes) {
		this.id = id;
		this.layer = layer;
		this.name = name;
		this.houseNumber = houseNumber;
		this.ref = ref;
		this.tags = tags;
		this.shape = shape;
		this.wayNodes = wayNodes;
	}

	/**
	 * Merges tags from a relation with the tags of this way and puts the result into the way tags of this way.
	 * 
	 * @param relation
	 *            the relation
	 */
	// public void mergeRelationInformation(TDRelation relation) {
	// if (relation.hasTags()) {
	// addTags(relation.getTags());
	// }
	// if (getName() == null && relation.getName() != null) {
	// setName(relation.getName());
	// }
	// if (getRef() == null && relation.getRef() != null) {
	// setRef(relation.getRef());
	// }
	// }

	/**
	 * @return the zoom level this entity appears first
	 */
	public byte getMinimumZoomLevel() {
		return OSMTagMapping.getInstance().getZoomAppearWay(this.tags);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the house number
	 */
	public String getHouseNumber() {
		return this.houseNumber;
	}

	/**
	 * @return the ref
	 */
	public String getRef() {
		return this.ref;
	}

	/**
	 * @param ref
	 *            the ref to set
	 */
	public void setRef(String ref) {
		this.ref = ref;
	}

	/**
	 * @return the tags
	 */
	public short[] getTags() { // NOPMD by bross on 25.12.11 13:04
		return this.tags; // NOPMD by bross on 25.12.11 13:07
	}

	/**
	 * @param tags
	 *            the tags to set
	 */
	public void setTags(short[] tags) { // NOPMD by bross on 25.12.11 13:04
		this.tags = tags;
	}

	/**
	 * @return the shape
	 */
	public byte getShape() {
		return this.shape;
	}

	/**
	 * @param shape
	 *            the shape to set
	 */
	public void setShape(byte shape) {
		this.shape = shape;
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return this.id;
	}

	/**
	 * @return the layer
	 */
	public byte getLayer() {
		return this.layer;
	}

	/**
	 * @return true, if the way has tags
	 */
	public boolean hasTags() {
		return this.tags != null && this.tags.length > 0;
	}

	/**
	 * @return true, if the way is relevant for rendering
	 */
	public boolean isRenderRelevant() {
		return hasTags() || getName() != null && !getName().isEmpty() || getRef() != null && !getRef().isEmpty();
	}

//	private void addTags(short[] addendum) { // NOPMD by bross on 25.12.11 13:04
//		if (this.tags == null) {
//			this.tags = addendum;
//		} else {
//			TShortSet tags2 = new TShortHashSet();
//			tags2.addAll(this.tags);
//			tags2.addAll(addendum);
//			this.tags = tags2.toArray();
//		}
//
//	}

	/**
	 * @return true, if the way has at least 4 coordinates and the first and last coordinate are equal
	 */
	public boolean isPolygon() {
		return this.wayNodes != null && this.wayNodes.length >= GeoUtils.MIN_NODES_POLYGON
				&& this.wayNodes[0].getId() == this.wayNodes[this.wayNodes.length - 1].getId();
	}

	/**
	 * @return true, if the way represents a coastline
	 */
	public boolean isCoastline() {
		if (this.tags == null) {
			return false;
		}
		OSMTag tag;
		for (short tagID : this.tags) { // NOPMD by bross on 25.12.11 13:04
			tag = OSMTagMapping.getInstance().getWayTag(tagID);
			if (tag.isCoastline()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @return the way nodes
	 */
	public TDNode[] getWayNodes() {
		return this.wayNodes; // NOPMD by bross on 25.12.11 13:07
	}

	/**
	 * @return true, if the way nodes have been reversed with respect to a particular relation
	 */
	public boolean isReversedInRelation() {
		return this.reversedInRelation;
	}

	/**
	 * @param reversedInRelation
	 *            set the flag that indicates whether the order of the way nodes are reversed by a particular relation
	 */
	public void setReversedInRelation(boolean reversedInRelation) {
		this.reversedInRelation = reversedInRelation;
	}

	/**
	 * @return true, if the way has a tag that forces a closed way to be a polygon line (instead of an area)
	 */
	public boolean isForcePolygonLine() {
		if (!hasTags()) {
			return false;
		}
		OSMTagMapping mapping = OSMTagMapping.getInstance();
		for (short tag : this.tags) { // NOPMD by bross on 25.12.11 13:05
			if (mapping.getWayTag(tag).isForcePolygonLine()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the invalid
	 */
	public boolean isInvalid() {
		return this.invalid;
	}

	/**
	 * @param invalid
	 *            the invalid to set
	 */
	public void setInvalid(boolean invalid) {
		this.invalid = invalid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (this.id ^ (this.id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TDWay other = (TDWay) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "TDWay [id=" + this.id + ", name=" + this.name + ", ref=" + this.ref + ", tags=" + Arrays.toString(this.tags) + ", polygon="
				+ this.shape + "]";
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}
}
