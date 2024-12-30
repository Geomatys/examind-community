/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package com.examind.database.api.jooq.tables;


import com.examind.database.api.jooq.Admin;
import com.examind.database.api.jooq.Indexes;
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.StyledDataRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function2;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.styled_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class StyledData extends TableImpl<StyledDataRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.styled_data</code>
     */
    public static final StyledData STYLED_DATA = new StyledData();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<StyledDataRecord> getRecordType() {
        return StyledDataRecord.class;
    }

    /**
     * The column <code>admin.styled_data.style</code>.
     */
    public final TableField<StyledDataRecord, Integer> STYLE = createField(DSL.name("style"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.styled_data.data</code>.
     */
    public final TableField<StyledDataRecord, Integer> DATA = createField(DSL.name("data"), SQLDataType.INTEGER.nullable(false), this, "");

    private StyledData(Name alias, Table<StyledDataRecord> aliased) {
        this(alias, aliased, null);
    }

    private StyledData(Name alias, Table<StyledDataRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.styled_data</code> table reference
     */
    public StyledData(String alias) {
        this(DSL.name(alias), STYLED_DATA);
    }

    /**
     * Create an aliased <code>admin.styled_data</code> table reference
     */
    public StyledData(Name alias) {
        this(alias, STYLED_DATA);
    }

    /**
     * Create a <code>admin.styled_data</code> table reference
     */
    public StyledData() {
        this(DSL.name("styled_data"), null);
    }

    public <O extends Record> StyledData(Table<O> child, ForeignKey<O, StyledDataRecord> key) {
        super(child, key, STYLED_DATA);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.STYLED_DATA_DATA_IDX, Indexes.STYLED_DATA_STYLE_IDX);
    }

    @Override
    public UniqueKey<StyledDataRecord> getPrimaryKey() {
        return Keys.STYLED_DATA_PK;
    }

    @Override
    public List<ForeignKey<StyledDataRecord, ?>> getReferences() {
        return Arrays.asList(Keys.STYLED_DATA__STYLED_DATA_STYLE_FK, Keys.STYLED_DATA__STYLED_DATA_DATA_FK);
    }

    private transient Style _style;
    private transient Data _data;

    /**
     * Get the implicit join path to the <code>admin.style</code> table.
     */
    public Style style() {
        if (_style == null)
            _style = new Style(this, Keys.STYLED_DATA__STYLED_DATA_STYLE_FK);

        return _style;
    }

    /**
     * Get the implicit join path to the <code>admin.data</code> table.
     */
    public Data data() {
        if (_data == null)
            _data = new Data(this, Keys.STYLED_DATA__STYLED_DATA_DATA_FK);

        return _data;
    }

    @Override
    public StyledData as(String alias) {
        return new StyledData(DSL.name(alias), this);
    }

    @Override
    public StyledData as(Name alias) {
        return new StyledData(alias, this);
    }

    @Override
    public StyledData as(Table<?> alias) {
        return new StyledData(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public StyledData rename(String name) {
        return new StyledData(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public StyledData rename(Name name) {
        return new StyledData(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public StyledData rename(Table<?> name) {
        return new StyledData(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Integer> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function2<? super Integer, ? super Integer, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function2<? super Integer, ? super Integer, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
