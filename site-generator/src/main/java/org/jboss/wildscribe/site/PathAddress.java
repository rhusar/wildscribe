/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.wildscribe.site;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * A path address for an operation.
 *
 * @author Brian Stansberry
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class PathAddress implements Iterable<PathElement> {

    /**
     * An empty address.
     */
    public static final PathAddress EMPTY_ADDRESS = new PathAddress(Collections.<PathElement>emptyList());

    /**
     * Creates a PathAddress from the given ModelNode address. The given node is expected to be an address node.
     *
     * @param node the node (cannot be {@code null})
     * @return the update identifier
     */
    public static PathAddress pathAddress(final ModelNode node) {
        if (node.isDefined()) {

//            final List<Property> props = node.asPropertyList();
            // Following bit is crap TODO; uncomment above and delete below
            // when bug is fixed
            final List<Property> props = new ArrayList<Property>();
            String key = null;
            for (ModelNode element : node.asList()) {
                Property prop = null;
                if (element.getType() == ModelType.PROPERTY || element.getType() == ModelType.OBJECT) {
                    prop = element.asProperty();
                } else if (key == null) {
                    key = element.asString();
                } else {
                    prop = new Property(key, element);
                }
                if (prop != null) {
                    props.add(prop);
                    key = null;
                }

            }
            if (props.size() == 0) {
                return EMPTY_ADDRESS;
            } else {
                final Set<String> seen = new HashSet<String>();
                final List<PathElement> values = new ArrayList<PathElement>();
                int index = 0;
                for (final Property prop : props) {
                    final String name = prop.getName();
                    if (seen.add(name)) {
                        values.add(new PathElement(name, prop.getValue().asString()));
                    } else {
                        throw duplicateElement(name);
                    }

                    index++;
                }
                return new PathAddress(Collections.unmodifiableList(values));
            }
        } else {
            return EMPTY_ADDRESS;
        }
    }

    public static PathAddress pathAddress(List<PathElement> elements) {
        if (elements.size() == 0) {
            return EMPTY_ADDRESS;
        }
        final ArrayList<PathElement> newList = new ArrayList<PathElement>(elements.size());
        final Set<String> seen = new HashSet<String>();
        int index = 0;
        for (PathElement element : elements) {
            final String name = element.getKey();
            if (seen.add(name)) {
                newList.add(element);
            } else {
                throw duplicateElement(name);
            }
            index++;

        }
        return new PathAddress(Collections.unmodifiableList(newList));
    }

    public static PathAddress pathAddress(PathElement... elements) {
        return pathAddress(Arrays.<PathElement>asList(elements));
    }

    public static PathAddress pathAddress(String key, String value) {
        return pathAddress(PathElement.pathElement(key, value));
    }

    public static PathAddress pathAddress(PathAddress parent, PathElement... elements) {
        List<PathElement> list = new ArrayList<PathElement>(parent.pathAddressList);
        for (PathElement element : elements) {
            list.add(element);
        }
        return pathAddress(list);
    }


    private static RuntimeException duplicateElement(final String name) {
        return new RuntimeException(String.format("Duplicate path element '%s' found", name));
    }

    private final List<PathElement> pathAddressList;

    PathAddress(final List<PathElement> pathAddressList) {
        assert pathAddressList != null;
        this.pathAddressList = pathAddressList;
    }

    /**
     * Gets the element at the given index.
     *
     * @param index the index
     * @return the element
     * @throws IndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    public PathElement getElement(int index) {
        final List<PathElement> list = pathAddressList;
        return list.get(index);
    }

    /**
     * Gets the last element in the address.
     *
     * @return the element, or {@code null} if {@link #size()} is zero.
     */
    public PathElement getLastElement() {
        final List<PathElement> list = pathAddressList;
        return list.size() == 0 ? null : list.get(list.size() - 1);
    }

    /**
     * Get a portion of this address using segments starting at {@code start} (inclusive).
     *
     * @param start the start index
     * @return the partial address
     */
    public PathAddress subAddress(int start) {
        final List<PathElement> list = pathAddressList;
        return new PathAddress(list.subList(start, list.size()));
    }

    /**
     * Get a portion of this address using segments between {@code start} (inclusive) and {@code end} (exclusive).
     *
     * @param start the start index
     * @param end   the end index
     * @return the partial address
     */
    public PathAddress subAddress(int start, int end) {
        return new PathAddress(pathAddressList.subList(start, end));
    }

    /**
     * Create a new path address by appending more elements to the end of this address.
     *
     * @param additionalElements the elements to append
     * @return the new path address
     */
    public PathAddress append(List<PathElement> additionalElements) {
        final ArrayList<PathElement> newList = new ArrayList<PathElement>(pathAddressList.size() + additionalElements.size());
        newList.addAll(pathAddressList);
        newList.addAll(additionalElements);
        return pathAddress(newList);
    }

    /**
     * Create a new path address by appending more elements to the end of this address.
     *
     * @param additionalElements the elements to append
     * @return the new path address
     */
    public PathAddress append(PathElement... additionalElements) {
        return append(Arrays.asList(additionalElements));
    }

    /**
     * Create a new path address by appending more elements to the end of this address.
     *
     * @param address the address to append
     * @return the new path address
     */
    public PathAddress append(PathAddress address) {
        return append(address.pathAddressList);
    }

    public PathAddress append(String key, String value) {
        return append(PathElement.pathElement(key, value));
    }

    public PathAddress append(String key) {
        return append(PathElement.pathElement(key));
    }

    /**
     * Navigate to this address in the given model node.
     *
     * @param model  the model node
     * @param create {@code true} to generate the last part of the node if it does not exist
     * @return the submodel
     * @throws NoSuchElementException if the model contains no such element
     */
    public ModelNode navigate(ModelNode model, boolean create) throws NoSuchElementException {
        final Iterator<PathElement> i = pathAddressList.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (create && !i.hasNext()) {
                if (element.isMultiTarget()) {
                    throw new IllegalStateException();
                }
                model = model.require(element.getKey()).get(element.getValue());
            } else {
                model = model.require(element.getKey()).require(element.getValue());
            }
        }
        return model;
    }

    /**
     * Navigate to, and remove, this address in the given model node.
     *
     * @param model the model node
     * @return the submodel
     * @throws NoSuchElementException if the model contains no such element
     */
    public ModelNode remove(ModelNode model) throws NoSuchElementException {
        final Iterator<PathElement> i = pathAddressList.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (i.hasNext()) {
                model = model.require(element.getKey()).require(element.getValue());
            } else {
                final ModelNode parent = model.require(element.getKey());
                model = parent.remove(element.getValue()).clone();
            }
        }
        return model;
    }

    /**
     * Convert this path address to its model node representation.
     *
     * @return the model node list of properties
     */
    public ModelNode toModelNode() {
        final ModelNode node = new ModelNode().setEmptyList();
        for (PathElement element : pathAddressList) {
            final String value;
            if (element.isMultiTarget() && !element.isWildcard()) {
                value = '[' + element.getValue() + ']';
            } else {
                value = element.getValue();
            }
            node.add(element.getKey(), value);
        }
        return node;
    }

    /**
     * Check whether this address applies to multiple targets.
     *
     * @return <code>true</code> if the address can apply to multiple targets, <code>false</code> otherwise
     */
    public boolean isMultiTarget() {
        for (final PathElement element : pathAddressList) {
            if (element.isMultiTarget()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the size of this path, in elements.
     *
     * @return the size
     */
    public int size() {
        return pathAddressList.size();
    }

    /**
     * Iterate over the elements of this path address.
     *
     * @return the iterator
     */
    @Override
    public ListIterator<PathElement> iterator() {
        return pathAddressList.listIterator();
    }

    public PathAddress getParent() {
        return subAddress(0, size() - 1);
    }

    @Override
    public int hashCode() {
        return pathAddressList.hashCode();
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof PathAddress && equals((PathAddress) other);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(PathAddress other) {
        return this == other || other != null && pathAddressList.equals(other.pathAddressList);
    }

    @Override
    public String toString() {
        return toModelNode().toString();
    }

    public String toCLIStyleString() {
        return toString('=');
    }

    public String toHttpStyleString() {
        return toString('/');
    }

    public String toPathStyleString() {
        return toString('/');
    }

    public static PathAddress parseCLIStyleAddress(String address) throws IllegalArgumentException {
        PathAddress parsedAddress = PathAddress.EMPTY_ADDRESS;
        if (address == null || address.trim().isEmpty() || address.equals("/")) {
            return parsedAddress;
        }
        String trimmedAddress = address.trim();
        if (trimmedAddress.charAt(0) != '/' || !Character.isAlphabetic(trimmedAddress.charAt(1))) {
            throw new RuntimeException(String.format("illigal CLI style address %s", address));
        }
        char[] characters = address.toCharArray();
        boolean escaped = false;
        StringBuilder keyBuffer = new StringBuilder();
        StringBuilder valueBuffer = new StringBuilder();
        StringBuilder currentBuffer = keyBuffer;
        for (int i = 1; i < characters.length; i++) {
            switch (characters[i]) {
                case '/':
                    if (escaped) {
                        escaped = false;
                        currentBuffer.append(characters[i]);
                    } else {
                        parsedAddress = addpathAddressElement(parsedAddress, address, keyBuffer, valueBuffer);
                        keyBuffer = new StringBuilder();
                        valueBuffer = new StringBuilder();
                        currentBuffer = keyBuffer;
                    }
                    break;
                case '\\':
                    if (escaped) {
                        escaped = false;
                        currentBuffer.append(characters[i]);
                    } else {
                        escaped = true;
                    }
                    break;
                case '=':
                    if (escaped) {
                        escaped = false;
                        currentBuffer.append(characters[i]);
                    } else {
                        currentBuffer = valueBuffer;
                    }
                    break;
                default:
                    currentBuffer.append(characters[i]);
                    break;
            }
        }
        parsedAddress = addpathAddressElement(parsedAddress, address, keyBuffer, valueBuffer);
        return parsedAddress;
    }

    private static PathAddress addpathAddressElement(PathAddress parsedAddress, String address, StringBuilder keyBuffer, StringBuilder valueBuffer) {
        if (keyBuffer.length() > 0) {
            if (valueBuffer.length() > 0) {
                return parsedAddress.append(PathElement.pathElement(keyBuffer.toString(), valueBuffer.toString()));
            }
            throw new RuntimeException(String.format("illigal CLI style address %s", address));
        }
        return parsedAddress;
    }

    private String toString(char keyValSeparator) {
        if (pathAddressList.size() == 0) {
            return "/";
        }
        StringBuilder sb = new StringBuilder();
        for (PathElement pe : pathAddressList) {
            sb.append('/');
            sb.append(pe.getKey());
            sb.append(keyValSeparator);
            sb.append(pe.getValue());
        }
        return sb.toString();
    }
}
