# skope


todo
# improve performance (cache at different quad levels
# handle animations
# look at spatial hash for faster perf: http://zufallsgenerator.github.io/2014/01/26/visually-comparing-algorithms/

####
Different reference implementations for processing skope data
- PostGIS for querying (slow, but sped up by multi-threading queries
- lucene faster, and sped up by caching by QuadTree from ( http://koti.mbnet.fi/ojalesa/quadtree/quadtree_intro.htm )