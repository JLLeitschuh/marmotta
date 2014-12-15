/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.marmotta.ldpath.model.fields;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.marmotta.ldpath.api.LDPathConstruct;
import org.apache.marmotta.ldpath.api.backend.NodeBackend;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.api.selectors.NodeSelector;
import org.apache.marmotta.ldpath.api.transformers.NodeTransformer;

/**
 * A field mapping maps a field name to a node selection and transforms it into the appropriate type.
 * <p/>
 * Author: Sebastian Schaffert <sebastian.schaffert@salzburgresearch.at>
 */
public class FieldMapping<T,Node> implements LDPathConstruct<Node> {

    private boolean isSimpleName = true;
    
    /**
     * The name of the field in the search index
     */
    private String fieldName;

    /**
     * The type of the field in the search index
     */
    private URI fieldType;

    /**
     * The selector to use for selecting nodes
     */
    private NodeSelector<Node> selector;

    /**
     * The transformer to use for generating values
     */
    private NodeTransformer<T,Node> transformer;

    /**
     * Additional config params for the (solr) field.
     */
    private Map<String, String> fieldConfig;

    private FieldMapping() {
    }
    
    public FieldMapping(URI fieldName, URI fieldType, NodeSelector<Node> selector, NodeTransformer<T,Node> transformer, Map<String, String> fieldConfig) {
        this(fieldName.toString(), fieldType, selector, transformer, fieldConfig);
        this.isSimpleName = false;
    }
    
    public FieldMapping(String fieldName, URI fieldType, NodeSelector<Node> selector, NodeTransformer<T,Node> transformer, Map<String, String> fieldConfig) {
    	this();
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.selector = selector;
        this.transformer = transformer;
        this.fieldConfig = fieldConfig;
    }


    public String getFieldName() {
        return fieldName;
    }

    public URI getFieldType() {
        return fieldType;
    }

    public NodeSelector<Node> getSelector() {
        return selector;
    }

    public void setSelector(NodeSelector<Node> selector) {
        this.selector = selector;
    }

    public NodeTransformer<T, Node> getTransformer() {
        return transformer;
    }

    public void setTransformer(NodeTransformer<T, Node> transformer) {
        this.transformer = transformer;
    }

    public Map<String, String> getFieldConfig() {
        return fieldConfig;
    }

    public void setFieldConfig(Map<String, String> fieldConfig) {
        this.fieldConfig = fieldConfig;
    }

    /**
     * Get the values of this mapping for the context node passed as argument, transformed into the
     * datatype generated by the transformer.
     *
     * @param backend
     * @param context
     * @return
     */
    public Collection<T> getValues(final RDFBackend<Node> backend, final Node context, Map<Node,List<Node>> paths) {
        Function<Node,T> function = new Function<Node, T>() {
            @Override
            public T apply(Node input) {
                return transformer.transform(backend,input, getFieldConfig());
            }
        };

        return Collections2.transform(selector.select(backend,context, ImmutableList.of(context), paths),function);
    }

    /**
     * Get the values of this mapping for the context node passed as argument, transformed into the
     * datatype generated by the transformer.
     *
     * @param backend
     * @param context
     * @return
     */
    public Collection<T> getValues(final RDFBackend<Node> backend, final Node context) {
        Function<Node,T> function = new Function<Node, T>() {
            @Override
            public T apply(Node input) {
                return transformer.transform(backend,input, getFieldConfig());
            }
        };

        return Collections2.transform(selector.select(backend,context, null, null),function);
    }

    
    
    public String getPathExpression(NodeBackend<Node> backend) {
        StringBuilder fc = new StringBuilder();
        if (fieldConfig != null) {
            fc.append("(");
            boolean first = true;
            for (Map.Entry<String, String> e : fieldConfig.entrySet()) {
                if (!first) {
                    fc.append(", ");
                }
                fc.append(e.getKey()).append("=\"").append(e.getValue()).append("\"");
                first = false;
            }
            fc.append(")");
        }
        if (isSimpleName)
            return String.format("%s = %s :: <%s>%s ;", fieldName, selector.getPathExpression(backend), fieldType, fc.toString());
        else
            return String.format("<%s> = %s :: <%s>%s ;", fieldName, selector.getPathExpression(backend), fieldType, fc.toString());
    }

}
