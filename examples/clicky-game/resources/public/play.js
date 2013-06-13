var context;

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

function one_image(image)
{
    var imageObj = new Image();
    imageObj.onload = function() { context.drawImage(imageObj,
						     image[0],
						     image[1]); };
    imageObj.src = image[2];
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
	    els[i][1].forEach(one_image);
	}
    }
}

function make_move(event)
{
    var canvas = document.getElementById('canvas');
    context = canvas.getContext('2d');
    var bounds = canvas.getBoundingClientRect();
    var x = event.clientX - bounds.left;
    var y = event.clientY - bounds.top;
    var req = new XMLHttpRequest();
    req.open("POST", "/move", true);
    req.onload = function(e) {draw_resp(JSON.parse(e.target.responseText))};
    req.send(JSON.stringify([x,y]));
}
