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



public class main implements Runnable, EventDrivenInput{


	boolean texturechange,ready,exiting,updated ;
	int horizontalviewdistance = 220,verticalviewdistance = 75 ;
	double maxraydistance = 20;
	int modelupdatesperframe = 2 ;
	int modellightupdatesperframe = 4 ;
	int cellsloadedpertick = 1 ;
	int lightupdatesperframe = 15000;
	double dt = 0 ;




	Thread mythread ;
	lwjglinputcatcher input ;
	static int windowwidth=1024,windowheight=768;
	static boolean fullscreen = false ;
	float frustumzoom = 1.5f;
	float[] bgcolor = new float[]{.21f,.22f,.2f} ;

	boolean mousegrabbed = false;
	boolean paused =false;


	public static Animation displayanimation = null;
	
	public static Quat4d rot = new Quat4d(0,0,0,1) ;
	double rotspeed = .001 ;


	public static void main(String args[]){
		//if atleast 2 arguments then first 2 are width and height of window
		if(args.length>=2){
			windowwidth = Integer.parseInt(args[0]);
			windowheight = Integer.parseInt(args[1]);
		}

		JFileChooser chooser = new JFileChooser("./");
		int returnVal = chooser.showOpenDialog(null);
		if(returnVal == JFileChooser.APPROVE_OPTION){
			
			String filename= chooser.getSelectedFile().getPath()  ;
			displayanimation = new Animation(new File(filename)) ;
		}
		
		
		main r = new main();



	}

	public main(){


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
							/*
							System.out.println("loading textures...");

							texturemanager.loadtexturesandtable(texturelocation,texturetablefile,0xffffffff) ;

							BufferedImage crackimage = texturemanager.loadimage(texturelocation +"cracks.PNG") ;
							texturemanager.settransparent(crackimage,0xffffffff) ;
							cracktexture = texturemanager.generateTexture(crackimage) ;
							sky = new skybox(texturelocation +"skybox.PNG") ;
							voxelcell.loadlightlevels(texturelocation +"lightlevels.PNG") ;
							texturechange=false;

							System.out.println("booting terrain loader...");

							init() ;

							 */
							texturechange = false ;
						}else{
							begin();
							
							
							//TODO move stuff here
							//rotate one frame by euler angles converting to quaternion and concatenating
							
							double dx = Mouse.getDX();
							double dy = Mouse.getDY();
							//System.out.println(dx +", " + dy) ;
							Quat4d newrot = new Quat4d(0,0,0,1) ;
							newrot.setAxisAngle(0, 1, 0, dx*rotspeed) ;
							newrot.mul(rot) ;
							//rot.mul(newrot) ;
							rot.setAxisAngle(1, 0, 0, dy*rotspeed) ;
							rot.mul(newrot) ;
							//convert quaternion to axis angle and apply
							double[] axisangle = rot.getaxisangle() ;
							//TODO Draw Stuff Here!
							//System.out.println("drawing");
							
							GL11.glTranslatef(-30, 0, -300) ;
							
							GL11.glRotatef((float)( axisangle[3]*180/Math.PI), (float)axisangle[0], (float)axisangle[1], (float)axisangle[2]) ;
							
								
						
							
						
							
							//GL11.glRotatef((float)(System.currentTimeMillis()&0xfffffff)/100f, 0, 1, 0) ;
							GL11.glColor3f(1, 1, 1) ; 
							//drawLine(new Vector3d(0,0,10), new Vector3d(10,10,10)) ;
						
							Joint root = displayanimation.getframe((System.currentTimeMillis()&0xfffff)/1000.0) ;
							//Joint root = displayanimation.baseskeletonroot ;
							root.setGlobalTransform();
							root.drawLines() ;
							
							
						}
					}
				}while(finish() );
			}
		}
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
				mousegrabbed=false;
			}


			if(keycode == Keyboard.KEY_BACK){


				paused = true ;
				/*
				sleep(20) ;
				mousegrabbed = false ;
				JFileChooser chooser = new JFileChooser("./");
				int returnVal = chooser.showSaveDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION){

					String filename= chooser.getSelectedFile().getPath()  ;

					System.out.println("Save Successful. " + blocks +" blocks stored in " + zip.gzipped.filledsize+" bytes!");

				}

				player.activeplayer.character.mousegrabbed = true ;
				 */
				paused = false ;

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
			Mouse.setGrabbed(true);
			mousegrabbed=true;


		

		}

		if(button == 0){
			if(state){

			}else{

			}
		}else if(button == 1){
			if(state){

			}else{

			}
		}
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


			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_DEPTH_TEST);



			GL11.glMatrixMode(GL11.GL_PROJECTION);

			/*
			System.err.println("GL_VENDOR: " + GL11.glGetString(GL11.GL_VENDOR));
			System.err
					.println("GL_RENDERER: " + GL11.glGetString(GL11.GL_RENDERER));
			System.err.println("GL_VERSION: " + GL11.glGetString(GL11.GL_VERSION));
			System.err.println();
			System.err.println("glLoadTransposeMatrixfARB() supported: "
					+ GLContext.getCapabilities().GL_ARB_transpose_matrix);
			 */
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

			/*
			frustum = new plane[]{
					new plane(new double[][]{new double[]{0,0,0}, new double[]{-widthperheight,-1,-frustumzoom}, new double[]{-widthperheight,1,-frustumzoom}}),//left
					new plane(new double[][]{new double[]{0,0,0}, new double[]{widthperheight,-1,-frustumzoom}, new double[]{widthperheight,1,-frustumzoom}}),//right
					new plane(new double[][]{new double[]{0,0,0}, new double[]{-widthperheight,-1,-frustumzoom}, new double[]{widthperheight,-1,-frustumzoom}}),//top
					new plane(new double[][]{new double[]{0,0,0}, new double[]{-widthperheight,1,-frustumzoom}, new double[]{widthperheight,1,-frustumzoom}}),//bottom
					new plane(new double[][]{new double[]{0,0,-horizontalviewdistance}, new double[]{0,1,-horizontalviewdistance}, new double[]{1,0,-horizontalviewdistance}}),//far
			} ;

			double visiblepoint[] = new double[]{0,0,-1};
			for(int k=0;k<frustum.length;k++){
				if(frustum[k].signeddistance(visiblepoint)<0){
					frustum[k].flip();
				}
			}*/


			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			// GL11.glOrtho(0.0, 1.0, 0.0, 1.0, -1.0, 1.0);
			GL11.glClearDepth(1.0);



		} catch (Exception e) {
			return false;
		}
		return true;
	}


	public static void begin() {
		GL11.glDisable(GL11.GL_LIGHTING);

		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glLoadIdentity();
		// GL11.glPushMatrix();
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
