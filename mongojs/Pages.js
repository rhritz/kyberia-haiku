// manually run this in the mongo console
// in the future we will do this automatically
// you can remove the _ids, mongo will generate new ones
// and we don't depend on the values'

db.Page.save({ "_id" : ObjectId("4c801433a45520a4ae535695"),
    "className" : "models.Page", "name" : "main",
    "template" : "/app/views/Pages/main.html",
    "owner" : ObjectId("4ba5454aca05e9aabcc442ae")
});

db.Page.save({ "_id" : ObjectId("4c93dae3bbc720a4d1afc075"),
    "className" : "models.Page", "name" : "test",
    "template" : "/app/views/Pages/main.html",
    "owner" : ObjectId("4ba5454aca05e9aabcc442ae"),
    "blocks" : { "models.feeds.LastNodes" : "nodes2",
                "models.feeds.UserNodeChildren" : "nodes"
                }
});

db.Page.save({ "_id" : ObjectId("4c94faae8f1b20a40494e158"),
     "className" : "models.Page", "name" : "Last",
     "template" : "/app/views/Application/showLast.html",
     "owner" : ObjectId("4ba5454aca05e9aabcc442ae"),
     "blocks" : { "models.feeds.LastNodes" : "nodes" }
 });

db.Page.save({ "_id" : ObjectId("4c94fdc84cd120a4311f35b3"),
    "className" : "models.Page", "name" : "Friends Nodes",
    "template" : "/app/views/Application/showNodes.html",
    "owner" : ObjectId("4ba5454aca05e9aabcc442ae"),
    "blocks" : { "models.feeds.FriendsNodes" : "nodes" }
});

db.Page.save({ "_id" : ObjectId("4c950208ba6920a434f06adc"),
    "className" : "models.Page", "name" : "K",
    "template" : "app/views/Application/showK.html",
    "owner" : ObjectId("4c04144139dd20a4233e9f46"),
    "blocks" : { "models.feeds.KList" : "nodes" }
});

db.Page.save({ "_id" : ObjectId("4c95040eaaa320a48e0546df"),
    "className" : "models.Page", "name" : "Node",
    "template" : "/app/views/Application/viewNode.html",
    "owner" : ObjectId("4c04144139dd20a4233e9f46")
});

db.Page.save({ "_id" : ObjectId("4c950a15ec5e20a460077ddc"),
    "className" : "models.Page", "name" : "Tags",
    "template" : "app/views/Application/showTags.html",
    "owner" : ObjectId("4c04144139dd20a4233e9f46"),
    "blocks" : { "models.feeds.Tags" : "tags" }
});



