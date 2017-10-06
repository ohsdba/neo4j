/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.internal.kernel.api;

import org.junit.Test;

import org.neo4j.collection.primitive.Primitive;
import org.neo4j.collection.primitive.PrimitiveLongSet;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.neo4j.graphdb.RelationshipType.withName;
import static org.neo4j.values.storable.Values.stringValue;

public abstract class ExplicitIndexCursorTestBase<G extends KernelAPIReadTestSupport>
        extends KernelAPIReadTestBase<G>
{
    @Override
    void createTestGraph( GraphDatabaseService graphDb )
    {
        try ( Transaction tx = graphDb.beginTx() )
        {
            graphDb.index().forNodes( "foo" ).add( graphDb.createNode(), "bar", "this is it" );
            Relationship edge = graphDb.createNode().createRelationshipTo( graphDb.createNode(), withName( "LALA" ) );
            graphDb.index().forRelationships( "rels" ).add( edge, "alpha", "betting on the wrong string" );

            tx.success();
        }
    }

    @Test
    public void shouldFindNodeByLookup() throws Exception
    {
        // given
        try ( NodeExplicitIndexCursor cursor = cursors.allocateNodeManualIndexCursor();
              PrimitiveLongSet nodes = Primitive.longSet() )
        {
            // when
            read.nodeExplicitIndexLookup( cursor, "foo", "bar", stringValue( "this is it" ) );

            // then
            assertFoundNodes( cursor, 1, nodes );

            // when
            read.nodeExplicitIndexLookup( cursor, "foo", "bar", stringValue( "not that" ) );

            // then
            assertFoundNodes( cursor, 0, nodes );
        }
    }

    @Test
    public void shouldFindNodeByQuery() throws Exception
    {
        // given
        try ( NodeExplicitIndexCursor cursor = cursors.allocateNodeManualIndexCursor();
              PrimitiveLongSet nodes = Primitive.longSet() )
        {
            // when
            read.nodeExplicitIndexQuery( cursor, "foo", "bar:this*" );

            // then
            assertFoundNodes( cursor, 1, nodes );

            // when
            nodes.clear();
            read.nodeExplicitIndexQuery( cursor, "foo", "bar", "this*" );

            // then
            assertFoundNodes( cursor, 1, nodes );

            // when
            read.nodeExplicitIndexQuery( cursor, "foo", "bar:that*" );

            // then
            assertFoundNodes( cursor, 0, nodes );

            // when
            read.nodeExplicitIndexQuery( cursor, "foo", "bar", "that*" );

            // then
            assertFoundNodes( cursor, 0, nodes );
        }
    }

    @Test
    public void shouldFindRelationshipByLookup() throws Exception
    {
        // given
        try ( RelationshipExplicitIndexCursor cursor = cursors.allocateRelationshipManualIndexCursor();
              PrimitiveLongSet edges = Primitive.longSet() )
        {
            // when
            read.relationshipExplicitIndexGet(
                    cursor,
                    "rels",
                    "alpha",
                    stringValue( "betting on the wrong string" ),
                    -1,
                    -1 );

            // then
            assertFoundRelationships( cursor, 1, edges );
        }
    }

    static void assertFoundNodes( NodeIndexCursor node, int nodes, PrimitiveLongSet uniqueIds )
    {
        for ( int i = 0; i < nodes; i++ )
        {
            assertTrue( "at least " + nodes + " nodes", node.next() );
            assertTrue( uniqueIds.add( node.nodeReference() ) );
        }
        assertFalse( "no more than " + nodes + " nodes", node.next() );
    }

    static void assertFoundRelationships( RelationshipIndexCursor edge, int edges, PrimitiveLongSet uniqueIds )
    {
        for ( int i = 0; i < edges; i++ )
        {
            assertTrue( "at least " + edges + " relationships", edge.next() );
            assertTrue( uniqueIds.add( edge.relationshipReference() ) );
        }
        assertFalse( "no more than " + edges + " relationships", edge.next() );
    }
}
