# Troubleshooting Routing

## Graph Builder Data Import Issues

When you build a graph, OTP may encounter clearly incorrect or ambiguous data, or may detect less 
severe, but potentially problematic situations in the input data. Such problems should result in a 
"Data Import Issue" being generated. These issues are logged the the `DATA_IMPORT_ISSUES` console 
logger, depending on your need you might turn this logger on/off. At the end of the graph build 
process, OTP prints a summary of all the issues, like the following:

 ```
 11:35:57.515 INFO (Graph.java:970) Summary (number of each type of issues):
 11:35:57.518 INFO (Graph.java:976)     TurnRestrictionBad - 560
 11:35:57.518 INFO (Graph.java:976)     TurnRestrictionException - 15
 11:35:57.518 INFO (Graph.java:976)     StopLinkedTooFar - 22
 11:35:57.518 INFO (Graph.java:976)     HopSpeedSlow - 22
 11:35:57.518 INFO (Graph.java:976)     Graphwide - 1
 11:35:57.518 INFO (Graph.java:976)     GraphConnectivity - 407
 11:35:57.519 INFO (Graph.java:976)     ParkAndRideUnlinked - 1
 11:35:57.519 INFO (Graph.java:976)     StopNotLinkedForTransfers - 31
 11:35:57.519 INFO (Graph.java:976)     NoFutureDates - 1
```

The full set of issues can be written out to an HTML report for closer inspection. To enable the 
creation of these (potentially voluminous) HTML reports, add `"dataImportReport" : true` to your 
graph builder JSON configuration.

If the graph is saved to a file, these issues are saved with it and can be examined later. 
Currently the only tool for doing this is the "Graph Visualizer", which is not particularly well 
maintained and is intended for use by software developers familiar with OTP who can patch up the 
code as needed.


## Debug layers

OpenTripplanner has option to ease debugging problems with graph. Older option is graph visualizer.
Which you can enable with `--visualize` parameter instead of `--server` when starting OTP.
There you can see whole graph. You can click on edges and vertices and see the metadata. It is
 useful to see if street has expected options. And if connections are where they are expected.

It can be hard to use on large graphs since, whole graph is displayed at once. And it can be hard
 to search for specific streets since only street graph is shown without the rest of information.
 
 Another option is to use debug layers, which shows extra layers on top of normal map.
 To enable them you need to add `?debug_layers=true` to URL. For example 
 [http://localhost:8080/?debug_layers=true](http://localhost:8080/?debug_layers=true).
  This adds debug layers to layer choosing dialog. Currently you can choose between:

- Wheelchair access (which colors street edges red if they don't allow wheelchair or green otherwise)
- Bike Safety (colors street edges based on how good are for cycling [smaller is better])
- Traversal permissions (colors street edges based on what types of transit modes are allowed to
 travel on them (Pedestrian, cycling, car are currently supported)) Traversal permissions layer also
 draws links from transit stops/bike rentals and P+R to graph. And also draws transit stops, bike rentals
  and P+R vertices with different color.

### Interpretation Traversal permissions layer

A sample traversal permissions layer looks like the following 
![screen shot 2015-06-26 at 11 45 22](https://cloud.githubusercontent.com/assets/4493762/8374829/df05c438-1bf8-11e5-8ead-c1dea41af122.png)
* Yellow lines is the link between a stop and the street graph.
* Grey lines are streets one can travel with the mode walk, bike, or car
* Green lines are paths one can travel with the mode walk only
* Red lines are streets one can travel with the mode car only
* Grey dots vertices where edges are connected. If two edges are crossing w/o a vertice at the intersection point, users will not be able to go from one street to the other. But this can be valid in case of over/under pass for 
example. If it's an error, it's usually caused by improperly connected OSM data (a shared OSM node is required). 

## OpenStreetMap Data

### Tags Affecting Permissions

Access tags (such as bicycle/foot = yes/no/designated) can be used to override default graph-building parameters. 

As a default, foot and bicycle traffic is ''not'' allowed on `highway=trunk`, `highway=trunk_link`, `highway=motorway`, `highway=motorway_link`, or `highway=construction`. 

Both *are* allowed on `highway=pedestrian`, `highway=cycleway`, and `highway=footway`. 

Finally, bicycles are *not*allowed on *highway=footway* when any of the following tags appear on a footway: `footway=sidewalk`, `public_transport=platform`, or `railway=platform`.

Other access tags (such as `access=no` and `access=private` affect routing as well, and can be overridden similarly. While `access=no` prohibits all traffic, `access=private` disallows through traffic.

See [osmWayPropertySet config attribute](Configuration.md#Way-property-sets)

### Railway Platforms

OTP users in Helsinki have documented their best practices for coding railway platforms in OpenStreetMap. These guidelines are available [in the OSM Wiki.](https://wiki.openstreetmap.org/wiki/Digitransit#Editing_railway_platforms)

### Further information
* [General information](https://github.com/opentripplanner/OpenTripPlanner/wiki/GraphBuilder#graph-concepts)
* [Bicycle routing](http://wiki.openstreetmap.org/wiki/OpenTripPlanner#Bicycle_routing)
* [Indoor mapping](https://github.com/opentripplanner/OpenTripPlanner/wiki/Indoor-mapping)
* [Elevators](http://wiki.openstreetmap.org/wiki/OpenTripPlanner#Elevators)
