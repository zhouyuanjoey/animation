import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ARBTransposeMatrix;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import org.lwjgl.util.glu.*;



public class main2 implements Runnable, EventDrivenInput{


	boolean texturechange,ready,exiting,updated ;






	Thread mythread ;
	lwjglinputcatcher input ;
	static int windowwidth,windowheight;
	static boolean fullscreen = false ;
	float frustumzoom = 1f;
	float[] bgcolor = new float[]{.21f,.22f,.2f} ;



	public static Animation displayanimation = null;

	static float sliderAABB[];//the location in raw screen coordinates of the time slider
	static float sliderwidth=10f, sliderheight=15f ;//slider mark size in pixels
	static float sliderposition = .5f; // the slider's position as a fraction o the slider bar
	static float interfacez = 10f ;

	static float playAABB[], pauseAABB[], stepleftAABB[], steprightAABB[],traceAABB[] ;
	static boolean playing = true ;
	static boolean tracingenabled = true ;
	long lastplaytime = System.currentTimeMillis() ;

	boolean mouseleftdown = false, mouserightdown = false  ;
	int lastmousex, lastmousey ;


	Vector3d camerapos = new Vector3d(30,50,200) ;
	static double animationAABB[] ;
	static Vector3d animationcenter ;
	double rotationspeed = .01 ;
	double zoomspeed = .2 ;

	static boolean modeldisplaymode = false ;
	
	
	static boundmodel model ;


	public static void main(String args[]){
		//if atleast 2 arguments then first 2 are width and height of window
		if(args.length>=2){
			windowwidth = Integer.parseInt(args[0]);
			windowheight = Integer.parseInt(args[1]);
		}else{
			windowwidth= 1024 ;
			windowheight = 768 ;
		}

		sliderAABB = new float[]{10,windowheight-30,windowwidth-150,windowheight-20} ;
		playAABB = new float[]{windowwidth-145,windowheight-35,windowwidth-125,windowheight-15} ;
		pauseAABB = new float[]{windowwidth-120,windowheight-35,windowwidth-100,windowheight-15} ;
		stepleftAABB = new float[]{windowwidth-95,windowheight-35,windowwidth-75,windowheight-15} ;
		steprightAABB = new float[]{windowwidth-70,windowheight-35,windowwidth-50,windowheight-15} ;
		traceAABB = new float[]{windowwidth-45,windowheight-35,windowwidth-25,windowheight-15} ;

		JFileChooser chooser = new JFileChooser("./");
		//load model
		int returnVal = chooser.showOpenDialog(null);
		Obj rawmodel=null ;
		if(returnVal == JFileChooser.APPROVE_OPTION){

			String filename= chooser.getSelectedFile().getPath() ;
			modeldisplaymode = true ;
			rawmodel = new Obj(new File(filename)) ;

			animationAABB = rawmodel.getAABB() ;
			animationcenter = new Vector3d((animationAABB[0]+animationAABB[3])/2, 
					(animationAABB[1]+animationAABB[4])/2,
					(animationAABB[2]+animationAABB[5])/2) ;

		}

		//load model skeleton
		returnVal = chooser.showOpenDialog(null);
		if(returnVal == JFileChooser.APPROVE_OPTION){
			String filename= chooser.getSelectedFile().getPath() ;
			displayanimation = new Animation(new File(filename)) ;
			model = new boundmodel(rawmodel, displayanimation.baseskeletonroot) ;
			
		}
		
		//load applied animation  skeleton
		returnVal = chooser.showOpenDialog(null);
		if(returnVal == JFileChooser.APPROVE_OPTION){
			String filename= chooser.getSelectedFile().getPath() ;
			displayanimation = new Animation(new File(filename)) ;
			
		}



		main2 r = new main2();



	}

	public main2(){


		texturechange = true ;
		ready = true ;
		mythread = new Thread(this);

		mythread.setPriority(Thread.NORM_PRIORITY) ;
		mythread.start();
		updated = true ;


	}




	public void run(){
		while(!exiting){//loop until told to exit

			if(updated){//when it's updated and not in window loop create a new window
				createWindow(windowwidth,windowheight,"BVH Display");
				Display.setVSyncEnabled(true);

				try{Thread.sleep(50);}catch(Exception e){}
				do{

					updated = false ;
					if(ready){

						if(texturechange){

							texturechange = false ;
						}else{

							begin();//begins section where 3D openGL drawing can happen




							//TODO Draw Stuff Here!
							double dx = Mouse.getX() - lastmousex;
							double dy = Mouse.getY() - lastmousey;
							if(mouseleftdown){
								//rotate camera around animation
								Vector4d rcp = new Vector4d(
										camerapos.x-animationcenter.x, 
										camerapos.y-animationcenter.y,
										camerapos.z-animationcenter.z,
										1) ;
								Quat4d newrot = new Quat4d(0,0,0,1) ;
								newrot.setAxisAngle(0, 1, 0, dx*rotationspeed) ;
								Matrix4d m = new Matrix4d(newrot, new Vector3d(), 1) ;
								m.transform(rcp) ;
								//calculate a relative x axis perpendicular to the facing vector and up
								Vector3d relxaxis = new Vector3d(rcp.x,rcp.y,rcp.z) ;
								relxaxis.cross(relxaxis,new Vector3d(0,1,0)) ;
								relxaxis.normalize();
								newrot.setAxisAngle(relxaxis.x, relxaxis.y, relxaxis.z, dy*rotationspeed) ;
								m = new Matrix4d(newrot, new Vector3d(), 1) ;
								m.transform(rcp) ;
								camerapos.add(animationcenter,new Vector3d(rcp.x,rcp.y,rcp.z)) ;
								/*

								//System.out.println(dx +", " + dy) ;
								Quat4d newrot = new Quat4d(0,0,0,1) ;
								newrot.setAxisAngle(0, 1, 0, dx*rotspeed) ;
								newrot.mul(rot) ;
								//rot.mul(newrot) ;
								rot.setAxisAngle(1, 0, 0, dy*rotspeed) ;
								rot.mul(newrot) ;
								 */
							}

							if(mouserightdown){
								//zoom camera in and out
								Vector3d rcp = new Vector3d() ;
								rcp.sub(camerapos,animationcenter ) ;
								double length = rcp.length() ;
								rcp.scale( (length + dy*-zoomspeed)/length, rcp) ;
								camerapos.add(animationcenter,rcp) ;
							}
							lastmousex = Mouse.getX() ;
							lastmousey = Mouse.getY() ;
							/*
							//convert quaternion to axis angle and apply
							double[] axisangle = rot.getaxisangle() ;

							//System.out.println("drawing");

							GL11.glTranslatef(-30, 0, -100) ;

							GL11.glRotatef((float)( axisangle[3]*180/Math.PI), (float)axisangle[0], (float)axisangle[1], (float)axisangle[2]) ;
							 */

							//camera always looks at middle of AABB containing animation
							GLU.gluLookAt((float)camerapos.x, (float)camerapos.y, (float)camerapos.z, 
									(float)animationcenter.x,(float)animationcenter.y,(float)animationcenter.z, 0, 1, 0) ;


							Joint root = displayanimation.getframe(sliderposition*displayanimation.animationlength) ;
							

							//GL11.glRotatef((float)(System.currentTimeMillis()&0xfffffff)/100f, 0, 1, 0) ;

							GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
							GL11.glEnable(GL11.GL_LIGHTING);
							GL11.glEnable(GL11.GL_LIGHT0);
							GL11.glEnable(GL11.GL_SMOOTH) ;
							model.pose(root).draw() ;

							GL11.glColor3f(1, 1, 1) ;

							long time = System.currentTimeMillis() ;
							if(playing){
								sliderposition += (time-lastplaytime)/(1000*displayanimation.animationlength) ;
								lastplaytime = time ;
								if(sliderposition > 1)sliderposition = 1 ;
							}else{
								lastplaytime = time ;
							}


							

							GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
							GL11.glEnable(GL11.GL_LIGHTING);
							GL11.glEnable(GL11.GL_LIGHT0);
							GL11.glColor3f(1, .5f, .5f) ;
							//draw the animation in cylindersa of "size", with "sides"
							root.drawCylinders(.5, 6) ;

							if(tracingenabled){
								GL11.glDisable(GL11.GL_LIGHTING);
								GL11.glColor3f(0,0.5f,1) ;
								int startoffset=-10,stopoffset = 3 ;
								for(int k=0;k<displayanimation.jointnames.size(); k++){
									String name = displayanimation.jointnames.get(k) ; // get the name of this joint
									Vector3d last = displayanimation.getframe(displayanimation.frametime*startoffset+ sliderposition*displayanimation.animationlength).getJoint(name).getWorldPoint() ;
									for(int f=startoffset+1; f < stopoffset; f++){
										Vector3d current = displayanimation.getframe(displayanimation.frametime*f+ sliderposition*displayanimation.animationlength).getJoint(name).getWorldPoint() ;
										//System.out.println(last.x +", " + last.y +", " + last.z) ;
										drawLine(last, current) ;
										last = current ;
									}
								}
							}


							//begin drawing iterface components here
							begininterface(windowwidth,windowheight) ;

							//the time slider 
							GL11.glColor3f(.4f, .4f, .4f) ;
							drawAABB(sliderAABB) ;

							//the little blip on the time slider
							GL11.glColor3f(.7f, .7f, .7f) ;
							GL11.glBegin(GL11.GL_QUADS) ;
							float cx = sliderAABB[0] +sliderposition*(sliderAABB[2]-sliderAABB[0]);
							float cy = 0.5f * (sliderAABB[1] + sliderAABB[3]) ;
							GL11.glVertex3f( cx - sliderwidth/2, cy-sliderheight/2, -interfacez) ;
							GL11.glVertex3f( cx + sliderwidth/2, cy-sliderheight/2, -interfacez) ;
							GL11.glVertex3f( cx + sliderwidth/2, cy+sliderheight/2, -interfacez) ;
							GL11.glVertex3f( cx - sliderwidth/2, cy+sliderheight/2, -interfacez) ;
							GL11.glEnd();

							//the other buttons
							//play 
							GL11.glColor3f(.4f, .4f, .4f) ;
							drawAABB(playAABB) ;
							GL11.glColor3f(.7f, .7f, .7f) ;
							GL11.glBegin(GL11.GL_TRIANGLES) ;
							GL11.glVertex3f( playAABB[0]  + .2f*(playAABB[2]-playAABB[0]), playAABB[1]  + .2f*(playAABB[3]-playAABB[1]), -interfacez) ;
							GL11.glVertex3f( playAABB[0]  + .2f*(playAABB[2]-playAABB[0]), playAABB[1]  + .8f*(playAABB[3]-playAABB[1]), -interfacez) ;
							GL11.glVertex3f( playAABB[0]  + .9f*(playAABB[2]-playAABB[0]), playAABB[1]  + .5f*(playAABB[3]-playAABB[1]), -interfacez) ;
							GL11.glEnd();

							//pause 
							GL11.glColor3f(.4f, .4f, .4f) ;
							drawAABB(pauseAABB) ;
							GL11.glColor3f(.7f, .7f, .7f) ;
							GL11.glBegin(GL11.GL_QUADS) ;
							GL11.glVertex3f( pauseAABB[0]  + .2f*(pauseAABB[2]-pauseAABB[0]), pauseAABB[1]  + .2f*(pauseAABB[3]-pauseAABB[1]), -interfacez) ;
							GL11.glVertex3f( pauseAABB[0]  + .4f*(pauseAABB[2]-pauseAABB[0]), pauseAABB[1]  + .2f*(pauseAABB[3]-pauseAABB[1]), -interfacez) ;
							GL11.glVertex3f( pauseAABB[0]  + .4f*(pauseAABB[2]-pauseAABB[0]), pauseAABB[1]  + .8f*(pauseAABB[3]-pauseAABB[1]), -interfacez) ;
							GL11.glVertex3f( pauseAABB[0]  + .2f*(pauseAABB[2]-pauseAABB[0]), pauseAABB[1]  + .8f*(pauseAABB[3]-pauseAABB[1]), -interfacez) ;

							GL11.glVertex3f( pauseAABB[0]  + .6f*(pauseAABB[2]-pauseAABB[0]), pauseAABB[1]  + .2f*(pauseAABB[3]-pauseAABB[1]), -interfacez) ;
							GL11.glVertex3f( pauseAABB[0]  + .8f*(pauseAABB[2]-pauseAABB[0]), pauseAABB[1]  + .2f*(pauseAABB[3]-pauseAABB[1]), -interfacez) ;
							GL11.glVertex3f( pauseAABB[0]  + .8f*(pauseAABB[2]-pauseAABB[0]), pauseAABB[1]  + .8f*(pauseAABB[3]-pauseAABB[1]), -interfacez) ;
							GL11.glVertex3f( pauseAABB[0]  + .6f*(pauseAABB[2]-pauseAABB[0]), pauseAABB[1]  + .8f*(pauseAABB[3]-pauseAABB[1]), -interfacez) ;

							GL11.glEnd();

							//step left
							GL11.glColor3f(.4f, .4f, .4f) ;
							drawAABB(stepleftAABB) ;
							GL11.glColor3f(.7f, .7f, .7f) ;
							GL11.glBegin(GL11.GL_QUADS) ;
							GL11.glVertex3f( stepleftAABB[0]  + .6f*(stepleftAABB[2]-stepleftAABB[0]), stepleftAABB[1]  + .2f*(stepleftAABB[3]-stepleftAABB[1]), -interfacez) ;
							GL11.glVertex3f( stepleftAABB[0]  + .8f*(stepleftAABB[2]-stepleftAABB[0]), stepleftAABB[1]  + .2f*(stepleftAABB[3]-stepleftAABB[1]), -interfacez) ;
							GL11.glVertex3f( stepleftAABB[0]  + .8f*(stepleftAABB[2]-stepleftAABB[0]), stepleftAABB[1]  + .8f*(stepleftAABB[3]-stepleftAABB[1]), -interfacez) ;
							GL11.glVertex3f( stepleftAABB[0]  + .6f*(stepleftAABB[2]-stepleftAABB[0]), stepleftAABB[1]  + .8f*(stepleftAABB[3]-stepleftAABB[1]), -interfacez) ;
							GL11.glEnd();
							GL11.glBegin(GL11.GL_TRIANGLES) ;
							GL11.glVertex3f( stepleftAABB[0]  + .5f*(stepleftAABB[2]-stepleftAABB[0]), stepleftAABB[1]  + .2f*(stepleftAABB[3]-stepleftAABB[1]), -interfacez) ;
							GL11.glVertex3f( stepleftAABB[0]  + .5f*(stepleftAABB[2]-stepleftAABB[0]), stepleftAABB[1]  + .8f*(stepleftAABB[3]-stepleftAABB[1]), -interfacez) ;
							GL11.glVertex3f( stepleftAABB[0]  + .2f*(stepleftAABB[2]-stepleftAABB[0]), stepleftAABB[1]  + .5f*(stepleftAABB[3]-stepleftAABB[1]), -interfacez) ;
							GL11.glEnd();
							//step right
							GL11.glColor3f(.4f, .4f, .4f) ;
							drawAABB(steprightAABB) ;
							GL11.glColor3f(.7f, .7f, .7f) ;
							GL11.glBegin(GL11.GL_QUADS) ;
							GL11.glVertex3f( steprightAABB[0]  + .4f*(steprightAABB[2]-steprightAABB[0]), steprightAABB[1]  + .2f*(steprightAABB[3]-steprightAABB[1]), -interfacez) ;
							GL11.glVertex3f( steprightAABB[0]  + .2f*(steprightAABB[2]-steprightAABB[0]), steprightAABB[1]  + .2f*(steprightAABB[3]-steprightAABB[1]), -interfacez) ;
							GL11.glVertex3f( steprightAABB[0]  + .2f*(steprightAABB[2]-steprightAABB[0]), steprightAABB[1]  + .8f*(steprightAABB[3]-steprightAABB[1]), -interfacez) ;
							GL11.glVertex3f( steprightAABB[0]  + .4f*(steprightAABB[2]-steprightAABB[0]), steprightAABB[1]  + .8f*(steprightAABB[3]-steprightAABB[1]), -interfacez) ;
							GL11.glEnd();
							GL11.glBegin(GL11.GL_TRIANGLES) ;
							GL11.glVertex3f( steprightAABB[0]  + .5f*(steprightAABB[2]-steprightAABB[0]), steprightAABB[1]  + .2f*(steprightAABB[3]-steprightAABB[1]), -interfacez) ;
							GL11.glVertex3f( steprightAABB[0]  + .5f*(steprightAABB[2]-steprightAABB[0]), steprightAABB[1]  + .8f*(steprightAABB[3]-steprightAABB[1]), -interfacez) ;
							GL11.glVertex3f( steprightAABB[0]  + .8f*(steprightAABB[2]-steprightAABB[0]), steprightAABB[1]  + .5f*(steprightAABB[3]-steprightAABB[1]), -interfacez) ;
							GL11.glEnd();
							//trace
							GL11.glColor3f(.4f, .4f, .4f) ;
							drawAABB(traceAABB) ;
							GL11.glColor3f(.7f, .7f, .7f) ;
							GL11.glBegin(GL11.GL_QUADS) ;
							GL11.glVertex3f( traceAABB[0]  + .8f*(traceAABB[2]-traceAABB[0]), traceAABB[1]  + .4f*(traceAABB[3]-traceAABB[1]), -interfacez) ;
							GL11.glVertex3f( traceAABB[0]  + .2f*(traceAABB[2]-traceAABB[0]), traceAABB[1]  + .4f*(traceAABB[3]-traceAABB[1]), -interfacez) ;
							GL11.glVertex3f( traceAABB[0]  + .2f*(traceAABB[2]-traceAABB[0]), traceAABB[1]  + .6f*(traceAABB[3]-traceAABB[1]), -interfacez) ;
							GL11.glVertex3f( traceAABB[0]  + .8f*(traceAABB[2]-traceAABB[0]), traceAABB[1]  + .6f*(traceAABB[3]-traceAABB[1]), -interfacez) ;
							GL11.glEnd();



						}
					}
				}while(finish() );
			}
		}
	}

	public static void drawAABB(float AABB[]){
		GL11.glBegin(GL11.GL_QUADS) ;
		GL11.glVertex3f(AABB[0], AABB[1], -interfacez-.001f) ;
		GL11.glVertex3f(AABB[2], AABB[1], -interfacez-.001f) ;
		GL11.glVertex3f(AABB[2], AABB[3], -interfacez-.001f) ;
		GL11.glVertex3f(AABB[0], AABB[3], -interfacez-.001f) ;
		GL11.glEnd();
	}

	public void drawLine(Vector3d a, Vector3d b){
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(a.x,a.y, a.z);
		GL11.glVertex3d(b.x,b.y, b.z);
		GL11.glEnd();

	}





	public void KeyEvent(int keycode, char keychar, boolean state) {
		if(state){
			if(keycode == Keyboard.KEY_ESCAPE){
				Mouse.setGrabbed(false);
			}


			if(keycode == Keyboard.KEY_BACK){



			}




			if(keycode == Keyboard.KEY_Z){
			}


		}
		if(keycode == Keyboard.KEY_W){
			//
		}
		if(keycode == Keyboard.KEY_A){

		}
		if(keycode == Keyboard.KEY_S){

		}
		if(keycode == Keyboard.KEY_D){

		}

		if(keycode == Keyboard.KEY_LSHIFT){

		}

		if(keycode == Keyboard.KEY_SPACE){

		}



		if(keycode == Keyboard.KEY_Q ){

		}


		if(keycode == Keyboard.KEY_E ){

		}



	}

	public synchronized void MouseEvent(int button, boolean state) {

		if(state){
			//Mouse.setGrabbed(true);
			//mousegrabbed=true;
			double x = Mouse.getX();
			double y = windowheight - Mouse.getY();
			//System.out.println(x +", " + y) ;
			//clicked slider
			if(inAABB(x,y,sliderAABB)){
				sliderposition=(Mouse.getX() -sliderAABB[0])/(sliderAABB[2] -sliderAABB[0]) ;
			}

			//hit play
			if(inAABB(x,y,playAABB)){
				playing=true ;
			}
			//hit pause
			if(inAABB(x,y,pauseAABB)){
				playing=false ;
			}

			//hit pause
			if(inAABB(x,y,traceAABB)){
				tracingenabled=!tracingenabled ;
			}

			//step one frame left
			if(inAABB(x,y,stepleftAABB)){
				sliderposition-= displayanimation.frametime/displayanimation.animationlength ;
				sliderposition = Math.max(sliderposition,0) ;
			}
			//step one frame right
			if(inAABB(x,y,steprightAABB)){
				sliderposition+= displayanimation.frametime/displayanimation.animationlength ;
				sliderposition = Math.min(sliderposition,1) ;
			}




		}

		if(button == 0){
			if(state){
				mouseleftdown = true ;
			}else{
				mouseleftdown = false ;
			}
		}else if(button == 1){
			if(state){
				mouserightdown = true ;
			}else{
				mouserightdown = false ;
			}
		}
		lastmousex = Mouse.getX() ;
		lastmousey = Mouse.getY() ;
	}


	public static boolean inAABB(double x, double y, float AABB[]){
		return x >= AABB[0] && x <= AABB[2] && y >=AABB[1] && y <=AABB[3] ;
	}


	public boolean createWindow(int w, int h, String title) {
		if(fullscreen){
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			windowwidth = dim.width ;
			windowheight = dim.height ;
		}else{
			windowwidth = w ;
			windowheight = h ;
		}
		try {
			if(fullscreen){
				Display.setFullscreen(true ) ;
			}else{
				Display.setDisplayMode(new DisplayMode(w, h));
			}
			Display.setTitle(title);
			Display.create();

			if(input!=null){//close previously running inputthread if necessarry
				input.exiting = true ;
			}
			input = new lwjglinputcatcher(this,20);//open new input catcher

			GL11.glClearColor(bgcolor[0], bgcolor[1], bgcolor[2], 0.0f);


			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_DEPTH_TEST);







			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			// GL11.glOrtho(0.0, 1.0, 0.0, 1.0, -1.0, 1.0);
			GL11.glClearDepth(1);



		} catch (Exception e) {
			return false;
		}
		return true;
	}


	public void begin() {

		GL11.glMatrixMode(GL11.GL_PROJECTION);

		if (!GLContext.getCapabilities().GL_ARB_transpose_matrix) {
			GL11.glLoadIdentity();
		} else {
			final FloatBuffer identityTranspose = BufferUtils
			.createFloatBuffer(16).put(
					new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0,
							0, 0, 0, 1 });
			identityTranspose.flip();
			ARBTransposeMatrix.glLoadTransposeMatrixARB(identityTranspose);
		}

		float widthperheight = windowwidth/(float)windowheight ;
		float nearplane = .17f ;
		GL11.glFrustum(-widthperheight*nearplane, widthperheight*nearplane, -1*nearplane, 1*nearplane, frustumzoom*nearplane, (float)(3000));

		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		// GL11.glOrtho(0.0, 1.0, 0.0, 1.0, -1.0, 1.0);
		GL11.glClearDepth(1);

		GL11.glDisable(GL11.GL_LIGHTING);

		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glLoadIdentity();
		// GL11.glPushMatrix();
	}

	public void begininterface(int w, int h) {


		GL11.glMatrixMode(GL11.GL_PROJECTION);
		if (!GLContext.getCapabilities().GL_ARB_transpose_matrix) {
			GL11.glLoadIdentity();
		} else {
			final FloatBuffer identityTranspose = BufferUtils
			.createFloatBuffer(16).put(
					new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0,
							0, 0, 0, 1 });
			identityTranspose.flip();
			ARBTransposeMatrix.glLoadTransposeMatrixARB(identityTranspose);
		}
		GL11.glFrustum(0, w, 0, h, interfacez, 100.0f);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		//GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glLoadIdentity();
		// do background objects

		GL11.glScalef(1,-1,1);
		GL11.glTranslatef(0, -h, 0);
		GL11.glDisable(GL11.GL_LIGHTING);
	}


	public boolean finish() {
		// GL11.glFlush();
		GL11.glFinish();
		// GL11.glPopMatrix();

		Display.update();

		if (Display.isCloseRequested()) {
			Display.destroy();
			// System.exit(0);
			exiting = true ;
			return false;
		}
		return true;

	}

	private void sleep(long ms){
		try{Thread.sleep(ms);}catch(Exception e){}
	}
}