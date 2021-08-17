# AutoMapper
Simple Java object mapper supporting builders


## TODO
* Deep mapping
* UTs on inner classes
* UTs on avro
* Register with given builder/build method names
* private props access ?
* mapping strategies depending on classes (~like what we are doing with avro)
* mapping shadowed props (needs GS to know the target classes in the hierarchy for a given prop)
* setBuilderContext is mutable, shouldn't be, or not so easy to set
* allow coertion (like in EL) or not