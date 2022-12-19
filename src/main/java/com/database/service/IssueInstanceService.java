package com.database.service;

import com.database.object.IssueInstance;

import java.util.List;

public interface IssueInstanceService {

    int insert(IssueInstance issueInstance);

    IssueInstance getInstById(int inst_id);

    List<IssueInstance> getInstByCommit(int commit);

}