package sonar;

import utils.CmdUtil;
import cn.edu.fudan.issue.entity.dbo.Location;
import cn.edu.fudan.issue.entity.dbo.RawIssue;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Sonar {

    //扫描单个commit
    public void scannerCommit(String pathName, int repositoryId, int branchId, int commitId) throws IOException {
        String cdStr = "cmd /c cd " + pathName;
        String componentKeys = "repositoryId" + repositoryId + "_" + "branchId" + branchId + "_" + "commitId" + commitId;
        String scannerStr = "sonar-scanner -D sonar.projectKey=" + componentKeys;
        System.out.println(componentKeys);
        System.out.println(CmdUtil.run(cdStr + " && " + scannerStr));
//                Process process = Runtime.getRuntime().exec(cdStr + " && " + scannerStr);
//                Process process = Runtime.getRuntime().exec(cdStr + " && dir");
//                process.waitFor( 10, TimeUnit.SECONDS);
    }

    //根据Id 从云端获取所有的Json, 并解析成RawIssue
    public List<RawIssue> getSonarResult(int repositoryId, int branchId, int commitId) throws Exception {
        List<RawIssue> resultRawIssues = new ArrayList<RawIssue>();

        JSONObject sonarIssueResult = getSonarIssueResults("repositoryId" + repositoryId + "_" + "branchId" + branchId + "_" + "commitId" + commitId, 1);
        //获取issue数量
        int pageSize = 100;
        int issueTotal = sonarIssueResult.getIntValue("total");
        int pages = issueTotal % pageSize > 0 ? issueTotal / pageSize + 1 : issueTotal / pageSize;
        for (int i = 1; i <= pages; i++) {
            JSONObject sonarResult = getSonarIssueResults("repositoryId" + repositoryId + "_" + "branchId" + branchId + "_" + "commitId" + commitId, i);
            JSONArray sonarRawIssues = sonarResult.getJSONArray("issues");

            for (int j = 0; j < sonarRawIssues.size(); j++) {
                JSONObject sonarIssue = sonarRawIssues.getJSONObject(j);

                String component = sonarIssue.getString("component");
                if (!component.endsWith(".java")) {
                    continue;
                }

                List<Location> locations = getLocations(sonarIssue);
                if (locations.isEmpty()) {
                    continue;
                }

                RawIssue rawIssue = new RawIssue();

                rawIssue.setType(sonarIssue.getString("type"));
                rawIssue.setFileName(getFileName(sonarIssue));
                rawIssue.setDetail(sonarIssue.getString("message"));
                rawIssue.setLocations(locations);
                rawIssue.setCommitId(getCommitHashByCommitId(commitId));

                String rawIssueUuid = RawIssue.generateRawIssueUUID(rawIssue);
                rawIssue.setUuid(rawIssueUuid);

                resultRawIssues.add(rawIssue);
            }
        }

        return resultRawIssues;
    }

    //根据Id 从云端获取Json数据
    private final String SEARCH_API = "http://127.0.0.1:9000/api/issues/search";
    private final String AUTHORIZATION = "Basic YWRtaW46MTIzNDU2Nzg=";
    private JSONObject getSonarIssueResults(String componentKeys, int page) throws IOException {

        String url_s = SEARCH_API + "?" + "componentKeys=" + componentKeys + "&page=" + page;
        URL url = new URL(url_s);

        URLConnection connection = url.openConnection();

        connection.setConnectTimeout(5000);
        connection.setReadTimeout(15000);

        connection.setRequestProperty("accept", "*/*");
        connection.setRequestProperty("connection", "Keep-Alive");
        connection.setRequestProperty("user-agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
        connection.setRequestProperty("authorization", AUTHORIZATION);

        connection.connect();

        //读返回值
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        String line;
        StringBuilder result = new StringBuilder();
        while ((line = in.readLine()) != null) {
            result.append(line);
        }
        in.close();

        return JSONObject.parseObject(result.toString());
    }

    //解析Json数据中的location信息
    private List<Location> getLocations(JSONObject issue) throws Exception {
        int startLine = 0;
        int endLine = 0;
        List<Location> locations = new ArrayList<>();
        JSONArray flows = issue.getJSONArray("flows");
        if (flows.size() == 0) {
            JSONObject textRange = issue.getJSONObject("textRange");
            if (textRange != null) {
                startLine = textRange.getIntValue("startLine");
                endLine = textRange.getIntValue("endLine");
            } else {
                return new ArrayList<>();
            }

            Location mainLocation = getLocation(startLine, endLine);
            locations.add(mainLocation);
        }
        else {
            for (int i = 0; i < flows.size(); i++) {
                JSONObject flow = flows.getJSONObject(i);
                JSONArray flowLocations = flow.getJSONArray("locations");

                for (int j = 0; j < flowLocations.size(); j++) {
                    JSONObject flowLocation = flowLocations.getJSONObject(j);
//                    String flowComponent = flowLocation.getString("component");
                    JSONObject flowTextRange = flowLocation.getJSONObject("textRange");
                    if (flowTextRange == null) {
                        continue;
                    }

                    int flowStartLine = flowTextRange.getIntValue("startLine");
                    int flowEndLine = flowTextRange.getIntValue("endLine");

                    Location location = getLocation(flowStartLine, flowEndLine);
                    locations.add(location);
                }
            }
        }

        return locations;
    }

    private Location getLocation(int startLine, int endLine) {
        Location location = new Location();
        location.setStartLine(startLine);
        location.setEndLine(endLine);

        return location;
    }


    //解析Json数据中的FileName
    private String getFileName(JSONObject issue) {
        String sonarPath;
        String[] sonarComponents;
        String filePath = null;

        sonarPath = issue.getString("component");
        if (sonarPath != null) {
            sonarComponents = sonarPath.split(":");
            if (sonarComponents.length >= 2) {
                filePath = sonarComponents[sonarComponents.length - 1];
            }
        }

        return filePath;
    }

}