package org.jmeterplugins.repository;

import junit.framework.AssertionFailedError;
import net.sf.json.*;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RepoTest {
    private final Set<String> cache = new HashSet<>();
    private String s = File.separator;
    private File repo = new File(System.getProperty("project.build.directory", "target") + s + "jpgc-repo");
    private File lib = new File(repo.getAbsolutePath() + s + "lib");
    private File libExt = new File(lib.getAbsolutePath() + s + "ext");

    public RepoTest() {
        try {
            FileUtils.deleteDirectory(lib);
        } catch (IOException e) {
            e.printStackTrace();
        }
        libExt.mkdirs();
    }

    @Test
    public void testAll() throws IOException {
        Map<String, String> env = System.getenv();
        if (env.containsKey("TRAVIS")) {
            System.out.println("Not running test inside Travis CI");
            return;
        }

        List<String> problems = new ArrayList<>();
        File[] files = getRepoFiles();

        JSONArray merged = new JSONArray();
        for (File repoFile : files) {
            System.out.println("Checking repo: " + repoFile.getCanonicalPath());
            String content = new String(Files.readAllBytes(Paths.get(repoFile.getAbsolutePath())), "UTF-8");
            JSON json = JSONSerializer.toJSON(content, new JsonConfig());
            JSONArray list = (JSONArray) json;
            for (Object item : list) {
                JSONObject spec = (JSONObject) item;
                checkPlugin(problems, repoFile, spec);

                JSONObject vers = spec.getJSONObject("versions");
                if (vers.getJSONObject("").isEmpty()) {
                    merged.add(spec);
                }

                break; // FIXME
            }
        }

        if (problems.size() > 0) {
            throw new AssertionFailedError(problems.toString());
        }

        PrintWriter out = new PrintWriter(new File(repo.getAbsolutePath() + s + "all.json"));
        out.print(merged.toString(1));
        out.close();
    }

    private File[] getRepoFiles() throws IOException {
        String path = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        String up = File.separator + "..";
        String repos = path + up + up + up + File.separator + "site" + File.separator + "dat" + File.separator + "repo";
        File dir = new File(repos);

        System.out.println("Working with " + dir.getCanonicalPath());
        File[] files = dir.listFiles();
        assert files != null;
        return files;
    }

    private void checkPlugin(List<String> problems, File repoFile, JSONObject spec) {
        Plugin plugin = Plugin.fromJSON(spec);
        if (plugin.isVirtual()) {
            return;
        }

        String maxVersion = plugin.getMaxVersion();
        JSONObject maxVerObject = spec.getJSONObject("versions").getJSONObject(maxVersion);

        JSONObject newVersions = new JSONObject();
        newVersions.put(maxVersion, maxVerObject);

        try {
            System.out.println("Checking plugin: " + plugin);
            plugin.setCandidateVersion(maxVersion);
            plugin.download(dummy);

            File jar = new File(plugin.getTempName());
            File dest = new File(plugin.getDestName());
            File to = new File(libExt.getAbsolutePath() + File.separator + dest.getName());
            jar.renameTo(to);
            if (!maxVerObject.isEmpty()) {
                maxVerObject.put("downloadUrl", "lib/ext/" + dest.getName());
            }
        } catch (Throwable e) {
            problems.add(repoFile.getName() + ":" + plugin);
            System.err.println("Problem with " + plugin);
            e.printStackTrace(System.err);
        }

        checkLibs(problems, repoFile, plugin);

        if (!maxVerObject.isEmpty()) {
            newVersions.put(maxVersion, maxVerObject);
            spec.put("versions", newVersions);
        }
    }

    private void checkLibs(List<String> problems, File repoFile, Plugin plugin) {
        Map<String, String> libs = plugin.getLibs(plugin.getCandidateVersion());
        for (String id : libs.keySet()) {
            if (!cache.contains(libs.get(id))) {
                try {
                    Downloader dwn = new Downloader(dummy);
                    String file = dwn.download(id, new URI(libs.get(id)));

                    File jar = new File(file);
                    File dest = new File(lib.getAbsolutePath() + File.separator + dwn.getFilename());
                    jar.renameTo(dest);

                    cache.add(libs.get(id));
                } catch (Throwable e) {
                    problems.add(repoFile.getName() + ":" + plugin + ":" + id);
                    System.err.println("Problem with " + id);
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    private GenericCallback<String> dummy = new GenericCallback<String>() {
        @Override
        public void notify(String s) {
        }
    };
}
