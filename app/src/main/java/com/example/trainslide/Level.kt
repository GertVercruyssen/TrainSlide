package com.example.trainslide

/**
 * Holds the data for loading a level
 * Parsed from txt file
 */
class Level(file: String) {
    lateinit var loadedParams: Map<String,Map<String, String>>
    init {
        parseLevel(file)
    }

    private fun parseLevel(filecontents: String) {
        //TODO: optimize in case levels get big
        var params = mutableMapOf<String, Map<String, String>>()
        val lines = filecontents.split(System.lineSeparator())
        for(line in lines) {
            if(!line.startsWith("//") && line != "") {
                val parts = line.split(":")
                val key = parts[0]
                val values = parts[1].split(",")
                var itemparams = mutableMapOf<String, String>()
                for (itemparam in values) {
                    itemparams[itemparam.split("=")[0]] = itemparam.split("=")[1]
                }
                params[key] = itemparams.toMap()
            }
        }
        loadedParams = params.toMap()
    }

    /**
     * Returns a list of items defined in the level, each item has a map for parameters
     * the first element in a level description must always be unique!
     */
    fun getParamsforSpecific(thing: String): Map<String,String> = loadedParams[thing]!!

    /**
     * Returns a list of items defined in the level, each item has a map for parameters
     */
    public fun getParamsfor(thing: String): Map<String, Map<String,String>> {
        return loadedParams.filterKeys { it.contains(thing) }
    }
}