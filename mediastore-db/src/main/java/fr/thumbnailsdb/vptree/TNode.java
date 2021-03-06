package fr.thumbnailsdb.vptree;

import java.io.Serializable;

/**
 * @author Paolo Ciccarese
 */
public class TNode implements Serializable {

    private static final long serialVersionUID = -217604190976851241L;

    private final Serializable obj;
    private double median;
    private TNode left;
    private TNode right;

    /**
     * The Object will be fixed during the instantiation of the node, while the
     * children will be defined in another iteration of the algorithm,
     */
    public TNode(Serializable obj) {
        this.obj = obj;
    }

    public Serializable get() {
        return this.obj;
    }

    public void setMedian(double median) {
        this.median = median;
    }

    public double getMedian() {
        return median;
    }

    public void setLeft(TNode leftNode) {
        this.left = leftNode;
    }

    public TNode getLeft() {
        return left;
    }

    public void setRight(TNode rightNode) {
        this.right = rightNode;
    }

    public TNode getRight() {
        return right;
    }
    
    public String toString() {
        return this.obj.toString();
    }
}
