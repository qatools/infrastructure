def projects = [
        'allure-framework/allure-core',
        'allure-framework/allure-report-builder',
        'allure-framework/allure-maven-plugin',
        'allure-framework/allure-teamcity-plugin',
        'allure-framework/allure-cli',
        'allure-framework/allure-cucumber-jvm-adaptor',
        'allure-framework/allure-spock-adaptor',
        'allure-examples/allure-examples-parent',
        'allure-examples/allure-testng-example',
        'allure-examples/allure-junit-example',
        'camelot-framework/clay',
        'camelot-framework/yatomata',
        'camelot-framework/yatomata-camel',
        'camelot-framework/camelot',
        'camelot-framework/camelot-utils',
        'yandex-qatools/ashot',
        'yandex-qatools/beanloader',
        'yandex-qatools/htmlelements',
        'yandex-qatools/matchers-java',
        'yandex-qatools/embedded-services',
        'yandex-qatools/postgresql-embedded',
        'yandex-qatools/pessimistic-mongodb',
        'qatools/properties',
        'qatools/opensource-parent',
        'jenkinsci/allure-plugin',
        'baev/javassist-classpath-scanner'
]

projects.each {
    println 'create job for ' + it

    def projectUrl = it
    def projectName = (projectUrl =~ /.*[\/](.*)/)[0][1]

    println 'project name ' + projectName

    mavenJob(projectName + '_master-deploy') {

        if (projectName.equals('properties') || projectName.equals('opensource-parent') || projectName.equals('pessimistic-mongodb') || projectName.equals('javassist-classpath-scanner')) {
            label('maven')
        } else {
            label('maven-old')
        }
        previousNames(projectName + '_deploy')
        scm {
            git {
                remote {
                    github projectUrl
                }
                branch('master')
                localBranch('master')
            }
        }
        triggers {
            githubPush()
        }

        goals 'org.jacoco:jacoco-maven-plugin:0.7.4.201502262128:prepare-agent clean deploy'

        wrappers {
            mavenRelease {
                releaseGoals('release:clean release:prepare release:perform')
                dryRunGoals('-DdryRun=true release:prepare')
                numberOfReleaseBuildsToKeep(10)
            }
        }

        configure { project ->
            project / publishers << 'hudson.plugins.sonar.SonarPublisher' {
                jdk('(Inherit From Job)')
                branch()
                language()
                mavenOpts("-Xmx1024m -Xms256m")
                jobAdditionalProperties()
                settings(class: 'jenkins.mvn.DefaultSettingsProvider')
                globalSettings(class: 'jenkins.mvn.DefaultGlobalSettingsProvider')
                usePrivateRepository(false)
            }
        }

        publishers {

            if (projectName.equals('allure-core')) {
                archiveArtifacts {
                    pattern('allure-commandline/target/*-standalone.*')
                }
                publishHtml {
                    report('allure-report-preview/target/allure-report/') {
                        reportName('Allure report')
                        keepAll()
                        alwaysLinkToLastBuild()
                    }
                }
            }

            if (projectName.equals('allure-teamcity-plugin')) {
                archiveArtifacts {
                    pattern('target/*.zip')
                }

            }
        }

    }

    mavenJob(projectName + '_pull-request') {
        label('maven-old')
        scm {
            git {
                remote {
                    github projectUrl
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                }
                branch('${sha1}')
            }
        }


        triggers {
            pullRequest {
                orgWhitelist(['allure-framework', 'qatools', 'yandex-qatools', 'allure-examples'])
                permitAll()
                useGitHubHooks()
            }
        }

        goals 'org.jacoco:jacoco-maven-plugin:0.7.4.201502262128:prepare-agent clean verify'

        configure { project ->
            project / publishers << 'hudson.plugins.sonar.SonarPublisher' {
                jdk('(Inherit From Job)')
                branch()
                language()
                mavenOpts("-Xmx1024m -Xms256m")
                jobAdditionalProperties('-Dsonar.analysis.mode=incremental -Dsonar.github.pullRequest=${ghprbPullId} -Dsonar.github.repository=' + projectUrl)
                settings(class: 'jenkins.mvn.DefaultSettingsProvider')
                globalSettings(class: 'jenkins.mvn.DefaultGlobalSettingsProvider')
                usePrivateRepository(false)
            }
        }

        publishers {

            if (projectName.equals('allure-core')) {
                archiveArtifacts {
                    pattern('allure-commandline/target/*-standalone.*')
                }
                publishHtml {
                    report('allure-report-preview/target/allure-report/') {
                        reportName('Allure report')
                        keepAll()
                        alwaysLinkToLastBuild()
                    }
                }
            }

            if (projectName.equals('allure-teamcity-plugin')) {
                archiveArtifacts {
                    pattern('target/*.zip')
                }

            }
        }

    }
}
