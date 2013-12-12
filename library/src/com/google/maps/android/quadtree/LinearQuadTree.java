package com.google.maps.android.quadtree;

import com.google.maps.android.geometry.Bounds;
import com.google.maps.android.geometry.Point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by irisu on 12/9/13.
 */
public class LinearQuadTree<T extends LinearQuadTree.Item> implements QuadTree<T> {

    private enum mQuadrant {
        TOP_LEFT(0),
        TOP_RIGHT(1),
        BOTTOM_LEFT(2),
        BOTTOM_RIGHT(3);

        final int numQuad;

        private mQuadrant(int i) {
            this.numQuad = i;
        }

        public int getValue(){
            return this.numQuad;
        }
    }

    private class Node implements Comparable<Node> {
        public int location;
        public T t;

        public Node(T item) {
            location = getLocation(item.getPoint());
            this.t = item;
        }

        public Node(int location) {
            this.location = location;
            this.t = null;
        }

        private int getLocation(Point p) {
            int location = 0;
            Bounds currBounds = mBounds;
            for (int order = mPrecision-1; order >= 0; order--) {
                if (p.y < currBounds.midY) {       // top
                    if (p.x < currBounds.midX) {   // left = 0
                        currBounds = new Bounds(currBounds.minX, currBounds.midX,
                                                     currBounds.minY, currBounds.midY);
                    } else {                       // right = 1
                        location += mQuadrant.TOP_RIGHT.getValue() * mBase ^order;
                        currBounds = new Bounds(currBounds.midX, currBounds.maxX,
                                                     currBounds.minY, currBounds.midY);
                    }
                } else {                           // bottom
                    if (p.x < currBounds.midX) {   // left = 2
                        location += mQuadrant.BOTTOM_LEFT.getValue() * mBase ^order;
                        currBounds = new Bounds(currBounds.minX, currBounds.midX,
                                                     currBounds.midY, currBounds.maxY);
                    } else {                       // right = 3
                        location += mQuadrant.BOTTOM_RIGHT.getValue() * mBase ^order;
                        currBounds = new Bounds(currBounds.midX, currBounds.maxX,
                                                     currBounds.midY, currBounds.maxY);
                    }
                }
            }
            return location;
        }

        public int compareTo(Node node) {
            return this.location - node.location;
        }

    }

    /**
     * The bounds of this quad.
     */
    private final Bounds mBounds;

    private ArrayList<Node> mPoints;

    public int mPrecision;

    public final int mBase = 4; // TODO change to binary?

    /**
     * Creates a new quad tree with specified bounds.
     *
     * @param minX
     * @param maxX
     * @param minY
     * @param maxY
     */
    public LinearQuadTree(double minX, double maxX, double minY, double maxY, int precision) {
        this(new Bounds(minX, maxX, minY, maxY), precision);
    }

    public LinearQuadTree(Bounds bounds, int precision) {
        if (precision > 25) precision = 25; // arbitrary maximum precision
        if (precision < 3)  precision = 3;  // arbitrary minimum precision
        mPoints = new ArrayList<Node>();
        mBounds = bounds;
        mPrecision = precision;
    }

    @Override
    public void add(T item) {
        Node node = new Node(item);
        int index = Collections.binarySearch(mPoints, node);
        mPoints.add(index, node);
    }

    @Override
    public boolean remove(T item) {
        Node node = new Node(item);
        int index = Collections.binarySearch(mPoints, node);
        if (mPoints.get(index) == node) {
            mPoints.remove(index);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        mPoints.clear();
    }

    @Override
    public Collection<T> search(Bounds searchBounds) {
        Collection<T> collection = new ArrayList<T>();
        search(searchBounds, mBounds, 0, mPrecision-1, collection);
        return collection;
    }

    // TODO: write the actual search
    private void search(Bounds searchBounds, Bounds currBounds,
                         int location, int depth, Collection<T> results) {
        if (searchBounds.contains(currBounds)) {
            // all the points in these bounds are in searchBounds
            Node node = new Node(location);
            int index = Collections.binarySearch(mPoints, node);
            for (; mPoints.get(index).location < location + (mBase^depth); index++) {
                results.add(mPoints.get(index).t);
            }
        } else if (searchBounds.intersects(currBounds) && depth>0) {
            // some of the points in these bounds are in searchBounds, can be split into quads
            search(searchBounds,
                    new Bounds(currBounds.minX, currBounds.midX, currBounds.minY, currBounds.midY),
                    location + mQuadrant.TOP_LEFT.getValue()*(mBase^depth), depth - 1, results);
            search(searchBounds,
                    new Bounds(currBounds.midX, currBounds.maxX, currBounds.minY, currBounds.midY),
                    location + mQuadrant.TOP_RIGHT.getValue()*(mBase^depth), depth - 1, results);
            search(searchBounds,
                    new Bounds(currBounds.minX, currBounds.midX, currBounds.midY, currBounds.maxY),
                    location + mQuadrant.BOTTOM_LEFT.getValue()*(mBase^depth), depth - 1, results);
            search(searchBounds,
                    new Bounds(currBounds.midX, currBounds.maxX, currBounds.midY, currBounds.maxY),
                    location + mQuadrant.BOTTOM_RIGHT.getValue()*(mBase^depth), depth - 1, results);

        } else if (searchBounds.intersects(currBounds)) {
            // some of the points in bounds are in searchBounds, quads can't be split
            for (Node node : mPoints) { // TODO change to only those in given quad
                results.add(node.t);
            }
        }
    }
}
