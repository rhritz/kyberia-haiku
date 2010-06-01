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
   nextnode = db.Node.findOne({_id:lastnode.dfs});
   if (nextnode == null) {
     return thread;
   }
   if (nextnode.par == null) {
     return thread;
   }
   parent = db.Node.findOne({_id:nextnode.par});
   if (parent == null) {
     return thread;
   }
   /* thread[thread.length] = [parent._id, lastnode._id, parent._id.str == lastnode._id.str]; */
   if (parent._id.str == lastnode._id.str) {
     roots[lastnode._id.str] = depth;
     depth++;
   } else {
     // ak v tomto bode nema parenta v roots,
     // znamena to ze siahame vyssie ako rootid - koncime
     if (roots[lastnode._id.str] == null) {
       return thread;
     }
     // nasli sme parenta, sme o jedno hlbsie ako on
     depth = roots[lastnode._id.str] + 1;
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