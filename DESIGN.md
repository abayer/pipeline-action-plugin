# Introduction
While a number of plugins now have compatibility with Pipeline, a non-trivial number still do not. What's worse, of those without
compatibility, a large swath, primarily consisting of Builder extensions (like Ant, Gradle, MSBuild, the Maven build step, etc) can't 
be natively ported to be compatible with Pipeline as they're currently written, since they would need to be represented as durable 
tasks and there are numerous issues around extending durable task further than shell and batch scripts. On top of that, most of the 
plugins that now do have compatibility don't necessarily have Pipeline steps - they just can be invoked via the "step" step in a 
non-intuitive manner - [for example, see here](https://github.com/jenkinsci/jenkins/blob/2.0/Jenkinsfile#L51). As a result, Pipeline 
scripts are more complicated than they need to be to get the job done right.

The Pipeline Action plugin is an attempt to help bridge some of the compatibility gap and provide a way for plugin authors (either of
existing plugins or new purely Pipeline script-written plugins) to contribute Pipeline script functions with a standard launching 
mechanism and standard argument style. 

# What the Pipeline Action plugin does/provides
* A "PipelineAction" extension point. Implementations of the extension point consist of a Groovy script (in 
src/main/resources/org/jenkinsci/plugins/etc…) that has a "call(Map args)" method, and a Groovy (or Java) Extension of PipelineAction, 
specifying:
  * the name of the Groovy class containing the Pipeline script
  * the alias for the action (i.e., "mvn" or "beanstalk-tomcat" or something along those lines)
  * the PipelineActionType for this action (currently just two - STANDARD by default, and NOTIFIER to mark whether the action is a
notifier - used by Plumber). The PipelineAction class itself handles all the logic for loading, parsing and providing the script to the
"runPipelineAction" Pipeline step's internals.
  * The known fields for the action arguments, if any, and whether they're required - defaults to an empty map.
    * Note - this doesn't mean you *have* to specify the fields, just that if you want to have a group of known fields to look for,
this is the place to do it. Will also be available to autocomplete (hopefully?) and reference docs eventually.
    * If a field is specified in the map and has "true" for its value, then it must be present in the passed arguments or the action 
will fail out.
  * An "AbstractPipelineActionScript" abstract class, which all the Groovy Pipeline classes actually implementing the behavior should
extend.
  * A new Pipeline step, "runPipelineAction(Map args)". A "name" key must be specified, with the value being the alias for a 
PipelineAction extension, while the remaining key/values in the map will be passed to the action script as arguments. This step 
originally was designed to be invoked within Plumber's execution, but can be used outside of Plumber just as easily.
    * Additional special-case PipelineActionTypes - the initial one is for notifiers, since in Plumber we don't want to be calling a 
"mvn" action when we need to be doing a "hipchat" notification. The PipelineActionType can be specified as the first argument to 
"runPipelineAction", either as a PipelineActionType enum value or as a string type. Without a type given, "runPipelineAction" defaults
to STANDARD.

# What this solves/enables
* Simpler way for providing Pipeline "steps" for plugins that can't easily be made compatible as is.
  * Example: a "mvn" action that can install Maven and Java, pass MAVEN_OPTS and goals/flags to a mvn execution (via sh/bat), and 
finally archive the artifacts and junit results, emulating the Maven freestyle step's behavior without needing to worry about durable
tasks, etc.
* Enables declarative approaches on top of Pipeline (i.e., Plumber).
* Provides an extension point for contributing larger blocks of Pipeline code that don't directly correspond to existing plugins
necessarily.

# Weaknesses
* Autocomplete won't work on the arguments currently (probably can be made to work with the getFields() method)
* New logic will be needed to render auto-generated documentation for actions - they aren't Pipeline steps exactly, so we have to make 
sure that we can generate that documentation from within the action itself.
* Questionable whether this is a weakness - since we aren't actually extending GlobalVariable (which is how the Docker Pipeline plugin 
and various POCs contribute Pipeline script currently), none of this will show up in the snippet generator currently. I'm open to 
alternative implementations that get us that (as well as autocomplete, etc) without hurting the core design of a consistent entry 
point/format for contributed Pipeline snippets.

# Examples

## Mail Notifier
https://github.com/abayer/pipeline-action-plugin/blob/master/src/main/java/io/jenkins/plugins/pipelineactions/actions/MailNotifier.java
https://github.com/abayer/pipeline-action-plugin/blob/master/src/main/resources/io/jenkins/plugins/pipelineaction/actions/MailNotifierImpl.groovy

## Invoking from pure Pipeline directly:
https://gist.github.com/abayer/36892845abd0ef4a593b
