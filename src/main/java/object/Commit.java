package object;

import java.util.Date;

public class Commit {
    private int commitId;

    private int branchId;

    private String hash;
    private Date commitTime;
    private String commiter;

//    public Commit(int commitId, branchId, hash, commitTime, commiter) {
//
//    }

    public Commit(int branchId, String hash, Date commitTime, String commiter) {
        this.branchId = branchId;
        this.hash = hash;
        this.commitTime = commitTime;
        this.commiter = commiter;
    }

    public Commit() {}

    public int getCommitId() {
        return commitId;
    }

    public void setCommitId(int commitId) {
        this.commitId = commitId;
    }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Date getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(Date commitTime) {
        this.commitTime = commitTime;
    }

    public String getCommiter() {
        return commiter;
    }

    public void setCommiter(String commiter) {
        this.commiter = commiter;
    }
}