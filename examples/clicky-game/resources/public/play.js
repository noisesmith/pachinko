function rectangles(rects, ctx)
{
    for(var i=0; i < rects.length; i++)
    {
	ctx.fillStyle=rects[i][0];
	ctx.fillRect(rects[i][1][0],
		     rects[i][1][1],
		     rects[i][1][2],
		     rects[i][1][3]);
    }
}

function images(images, context)
{
    for( var i = 0; i < images.length; i++ )
    {
	var im = images[i];
	var imageObj = new Image();
	imageObj.onload = function() { context.drawImage(imageObj,
							 im[0],
							 im[1]); };
	imageObj.src = im[2];
    }
}

function draw_resp(els)
{
    var canvas=document.getElementById('canvas');
    var context=canvas.getContext('2d');
    for( var i = 0; i < els.length; i++ )
    {
	if( els[i][0] == "rects" )
	{
	    rectangles(els[i][1], context);
	}
	else if (els[i][0] == "images" )
	{
	    images(els[i][1], context);
	}
    }
}

function make_move(event)
{
    var canvas = document.getElementById('canvas');
    var bounds = canvas.getBoundingClientRect();
    var x = event.clientX - bounds.left;
    var y = event.clientY - bounds.top;
    var req = new XMLHttpRequest();
    req.open("POST", "/move", true);
    req.onload = function(e) {draw_resp(JSON.parse(e.target.responseText))};
    req.send(JSON.stringify([x,y]));
}
