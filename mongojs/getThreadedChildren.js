// db.eval('return getThreadedChildren(ObjectId("4bf8b6c11e9f20a458e3b2c3"), 0, 30);');

db.system.js.save({_id: "getThreadedChildren", value:
function (rootid, start, count)
{
 thread = [];
 roots  = [];
 depth  = 0;
 nextnode = db.Node.findOne({_id:rootid});
 if (nextnode == null) {
   return thread;
 }
 for (i = 0; i < start + count; i++)
 {
   lastnode = nextnode;
   if (lastnode.dfs == null) {
     return thread;
   }
   nextnode = db.Node.findOne({_id:lastnode.dfs}); // TODO partial load?
   if (nextnode == null) {
     return thread;
   }
   if (nextnode.par == null) {
     return thread;
   }
   parent = nextnode.par.str;
   if (parent == lastnode._id.str) {
     roots[parent] = depth;
     depth++;
   } else {
     // ak v tomto bode nema parenta v roots,
     // znamena to ze siahame vyssie ako rootid - koncime
     if (roots[parent] == null) {
       return thread;
     }
     // nasli sme parenta, sme o jedno hlbsie ako on
     depth = roots[parent] + 1;
   }
   if ( i>= start) {
     // tento node chceme zobrazit
     // tu je vhodne miesto na kontrolu per-node permissions, ignore a fook zalezitosti
     thread[thread.length] = [nextnode._id,depth];
   }
 }
 return thread;
}
});