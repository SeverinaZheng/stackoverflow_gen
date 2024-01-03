# Docker File for LDBC-SNB Data Generator

Suppose you want to generate synthetic graph data compaible and loadable into a [Tinkerpop 3 data format](http://tinkerpop.apache.org/docs/current/reference/#_gremlin_i_o).
*We are here to the rescue!*

This repo contains files and configurations to:

* build a docker image with the desired data inside,
* transpose the LDBC-generated data to a Tinkerpop-compatible format, and
* load it into Tinkergraph.

Below a preamble, the *“just run it!”* steps are in the section **[RUN!](#run)**, but you should still read below to know what will happen. 

## Preamble

Below the basics of the process, in case you want to it yourself, but rest assured: *this docker image will make all the following automatically*.

### [LDBC-SNB Data Generator](https://github.com/ldbc/ldbc_snb_datagen)

>   The LDBC-SNB Data Generator (DATAGEN) is the responsible of providing the data sets used by all the LDBC benchmarks.
>   This data generator is designed to produce directed labeled graphs that mimic the characteristics of those graphs of real data.
>   A detailed description of the schema produced by datagen, as well as the format of the output files, can be found in the latest version of official [LDBC SNB specification document](https://github.com/ldbc/ldbc_snb_docs)

**Actually** what you care about is described as a [list of csv files](https://github.com/ldbc/ldbc_snb_datagen/wiki/Generated-CSV-Files).
Those `CSV` files will be generated ( *based on the configuration you provide, read below*) during the building of the image.


### The generation process

To obtain such data one should: 

1. install hadoop, and configure it;
2. download gremlin;
3. download `ldbc_gen` code, and configure it;
4. run the  `ldbc_gen` code to produce the `CSV` files;
5. parse it and convert it to a format compatible with tinkerpop 3.

Detailed steps are in the `Dockerfile`, sample configuration is in th `extra` directory.
Hence, the docker file, when building, will carry out steps 1 to 4, then when you run the docker image with th default command, it will perform the last step.

### Structure of the repo

~~~bash

    .
    ├── README.md                            # This document
    ├── images                               # Where the image code is
    │   ├── gremlin-ldbc_gen.dockerfile      # The hero of the story
    │   │      
    │   └── extra                            # Files needed in the setup
    │       ├── activate-sugar-tp3.groovy    # Sugar plug-in in the Tp3 Console
    │       ├── ldbc.large.params.ini        # Use this to get a large dataset
    │       ├── ldbc.params.ini              # Those are default, very small
    │       ├── mapred-site.xml              # The MEMORY conf. for Hadoop
    │       └── safe.sh                      # This as above: MEMORY 4 Hadoop
    │
    └── runtime                              # This folder will be loaded INSIDE
        │                                    # the docker image, if you need 
        │                                    # any file, put it here
        │
        ├── data                             # Output data will appear here
        └── ldbc.groovy                      # The parsing of th CSV and the 
                                             # conversion is performed by this 

~~~


## RUN!

The *zero step* is having docker up and running with all the required permissions for the current user.
The default configuration will try to use `4096MB` of main memory, and will generate a very tiny dataset for 100 users with 1 year of activity.
If you need more control, read more about [the Generator parameters](https://github.com/ldbc/ldbc_snb_datagen/wiki/Compilation_Execution), and about how to configure [memory for hadoop](https://hadoop.apache.org/docs/r2.7.2/hadoop-mapreduce-client/hadoop-mapreduce-client-core/mapred-default.xml).
Or you can see below.


### Configure

> There are two ways to configure the size of the desired data output: by setting the scale factor or by setting the number of persons, starting year and the number of years the data generated span. 

Standard configuration is for a very small dataset, with 100 users and 1 year of activiy (see `images/extra/ldbc.params.ini`)

~~~~bash
#...

ldbc.snb.datagen.generator.numPersons:100
ldbc.snb.datagen.generator.numYears:1

#...
~~~~

This will run easily with `1GB` of main memory.
If you want more data, change those two parameters, you can see a larger configuration in the `images/extra/ldbc.large.params.ini` file.


### Build the image

You can build the image, in `images`, with:


~~~bash
cd images
docker build -t gremlin/ldbc_gen -f gremlin-ldbc_gen.dockerfile .
~~~

This will do all the steps above, among which running hadoop and generating the data.
After gnerating the `.csv` files with hadoop the next step is to parse each one of them, and import their content into nodes and/or edges.
This second part is carried out by a gremlin/groovy script, the `runtime/ldbc.groovy`,  based upon the work of [Jonathan Ellithorpe (ellitron) at PlatformLab](https://github.com/PlatformLab/ldbc-snb-impls/blob/master/snb-interactive-titan/src/main/java/net/ellitron/ldbcsnbimpls/interactive/titan/TitanGraphLoader.java).
The scripts is run by the `gremlin.sh` console.
In this way, the image can be used a first time, with the default command (line 84 of `gremlin-ldbc_gen.dockerfile`:  `CMD ["gremlin.sh", "-e", "/runtime/ldbc.groovy"]`), to generate the Tinkerpop-compatible datasets.

So, to obtain the converted data, run, inside the root of the repo, the following command:

~~~bash
docker run  -v `pwd`/runtime:/runtime -e JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx4G' gremlin/ldbc_gen
~~~

The data is exported in two different formats to the directory `./runtime/data`.
You can read lines `450-464`  in `runtime/ldbc.groovy`, and comment out the part you do not need.


The third step, is to run it in interactive mode running the gremlin console.

~~~bash
docker run  -it -v `pwd`/runtime:/runtime -e JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx32G' gremlin/ldbc_gen gremlin.sh
~~~



### RUN A QUERY

You can also use the  docker image also to run query on top of the data.

~~~bash
docker run  -it -v `pwd`/runtime:/runtime -e JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx4G'  -e DATASET=/runtime/data/social_network.100u_1y.json  gremlin/ldbc_gen gremlin.sh -e /runtime/queries/count-edges.groovy
~~~



~~~adapted version
docker build -t gremlin/stackoverflow_gen -f gremlin-stackoverflow_gen.dockerfile .
docker run  -v `pwd`/runtime:/runtime -e JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx9G' gremlin/stackoverflow_gen
