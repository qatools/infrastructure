def projects = [
        'allure-framework/allure1',
        'allure-framework/allure-report-builder',
        'allure-framework/allure-maven-plugin',
        'allure-framework/allure-teamcity',
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
        'yandex-qatools/hamcrest-pojo-matcher-generator',
        'qatools/properties',
        'qatools/opensource-parent',
        'jenkinsci/allure-plugin',
        'seleniumkit/selenograph',
        'baev/javassist-classpath-scanner',
        'baev/junit.xml',
        'baev/jaxb-utils',
        'baev/hamcrest-optional'
]

projects.each {
    println 'create job for ' + it

    def projectUrl = it
    def projectName = (projectUrl =~ /.*[\/](.*)/)[0][1]

    println 'project name ' + projectName

    mavenJob(projectName + '_master-deploy') {

        if (projectName in ['properties','opensource-parent','pessimistic-mongodb','javassist-classpath-scanner','selenograph','junit.xml','jaxb-utils','hamcrest-optional']) {
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

        goals 'clean deploy'

        wrappers {
            mavenRelease {
                releaseGoals('release:clean release:prepare release:perform')
                dryRunGoals('-DdryRun=true release:prepare')
                numberOfReleaseBuildsToKeep(10)
            }
        }

        publishers {

            if (projectName.equals('allure1')) {
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

        goals 'clean verify'

        publishers {

            if (projectName.equals('allure1')) {
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
