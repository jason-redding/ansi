package com.witcraft.coveragecompliance

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.process.ExecSpec

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

class DiffCoverageCompliancePlugin implements Plugin<Project> {
    private static final String GIT_COMMAND_NAME = "git"
    private static final Pattern PATTERN_GIT_DIFF_NEW_FILE = ~'^\\+{3}\\s+(?<quote>["\']?)[ab][/\\\\](?<path>.+?)\\1\\t.*$'

    @Override
    void apply(Project project) {
        project.apply plugin: 'jacoco'
        project.apply plugin: 'com.form.diff-coverage'

        if (!project.gradle.startParameter.taskNames.contains('diffCoverage')) {
            return
        }

        String targetBranch = project.properties.getOrDefault('TARGET_BRANCH', '')
        if (targetBranch.empty) {
            project.logger.quiet "Task ':diffCoverage' is missing required property \"TARGET_BRANCH\". Skipping project"
            return
        }

        Path pathToGit = which(GIT_COMMAND_NAME)
        Path pathToGitRepo = findGitDirectory(project)
        if (Objects.isNull(pathToGitRepo)) {
            project.logger.quiet "Not within a git repository. Skipping project"
            return
        }
        if (Objects.isNull(pathToGit)) {
            project.logger.quiet "Could not find '${GIT_COMMAND_NAME}'. Skipping project"
            return
        }

        Path diffOutputPath = Paths.get(project.buildDir.toString(), 'diffCoverageCompliance', 'patch.diff')

        project.mkdir diffOutputPath.parent

        project.exec { ExecSpec e ->
            e.executable = pathToGit
            e.args = ['diff', '-p', '--merge-base', "--output=${diffOutputPath}", targetBranch, '--']
        }.assertNormalExitValue()

        if (Files.isReadable(diffOutputPath)) {
            List<Path> filesAffectedByDiff = Files.lines(diffOutputPath)
                .filter { it.startsWith("+++ ") }
                .map {
                    Matcher matcher = PATTERN_GIT_DIFF_NEW_FILE.matcher(it)
                    if (matcher.matches()) {
                        return Paths.get(pathToGitRepo, matcher.group("path"))
                    }
                    return null
                }
            Map<Project, Set<Path>> projectPathMap = collectByProject(project, null, filesAffectedByDiff)
            project.logger.quiet projectPathMap
        }
    }

    Map<Project, Set<Path>> collectByProject(Project project, Map<Project, Set<Path>> projectPathMap, List<Path> filesAffectedByDiff) {
        if (projectPathMap == null) {
            projectPathMap = new LinkedHashMap<>()
        }
        project.allprojects.each { Project p ->
            Path pathToAffectedFile = p.sourceSet.allJava.srcDirs
                .collect { it.toPath() }
                .findResult { Path srcDirToTry ->
                    filesAffectedByDiff.findResult { Path fileToTry ->
                        Path maybeFile = srcDirToTry.resolve(fileToTry)
                        return (Files.exists(maybeFile) ? maybeFile : null)
                    }
                }
            if (pathToAffectedFile != null) {
                projectPathMap.computeIfAbsent(p, { [] }).add(pathToAffectedFile)
            }
        }
    }

    final Path findGitDirectory(Project project) {
        Path tempPath = project.projectDir.toPath()
        do {
            Path gitDir = tempPath.resolve('.git')
            if (Files.isDirectory(gitDir)) {
                return gitDir
            }
        } while (Objects.nonNull(tempPath = tempPath.parent))
        return null
    }

    static final Path which(String name) {
        String envPath;
        String envPathExt = '';
        String PATH_SEPARATOR = System.getProperty('path.separator', ';')
        Map<String, String> env = System.getenv()
        env.keySet()
            .findAll { String key ->
                ("Path".equalsIgnoreCase(key) || "PathExt".equalsIgnoreCase(key))
            }
            .each { String key ->
                if ("Path".equalsIgnoreCase(key)) {
                    envPath = env.get(key).trim().replaceAll("\\s*${Pattern.quote(PATH_SEPARATOR)}\\s*", PATH_SEPARATOR)
                } else if ("PathExt".equalsIgnoreCase(key)) {
                    envPathExt = env.get(key).trim().toLowerCase().replaceAll('\\s*;\\s*', ';')
                }
            }

        if (envPathExt == null || envPathExt.empty) {
            envPathExt = '.exe;.sh'
        }

        final List<String> namesToTry = [name]
        envPathExt.tokenize(';').each {
            namesToTry.add "${name}${it}"
        }

        if (envPath != null && !envPath.empty) {
            return envPath.tokenize(PATH_SEPARATOR).findResult { String pathToTry ->
                namesToTry.findResult { String nameToTry ->
                    Path path = Paths.get(pathToTry, nameToTry)
                    return (Files.isExecutable(path) ? path : null)
                }
            }
        }
        return null
    }


}