import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Hashtable;


public class Animation {

	Joint baseskeletonroot ;
	public ArrayList<Frame> frames ;
	public double animationlength ; // the length of the animation
	public double frametime ; //the time of one frame
	
	static boolean verbose = false ;
	
	static final double degreestoradians = Math.PI/180 ;
	
	public ArrayList<String> jointnames;
	
	
	//loads an animation from a bvh file
	public Animation(File bvh){
		//build a list to keep track of what each number represents in a row
		ArrayList<String> parametertype = new ArrayList<String>() ;
		ArrayList<String> parameterobject = new ArrayList<String>() ;
		Joint currentparent = null ; // the current parent of joints being read
		String currentname = "" ; //the name of the next joint
		
		
		int frameamount ;
		try{
			BufferedReader br = new BufferedReader(new FileReader(bvh));
			int amountunnamed =0;
			
			while(br.ready()){
				String line = br.readLine().trim() ; // read line
				String l[] = line.split("\\s+");//split on whitespace
				//if(verbose)System.out.println(line) ;
				if(l[0].equals("ROOT") || l[0].equals("JOINT") ){//beginning of a joint
					//Don't actually need to do anything here since we wait until OFFSET to build and push the joint
					currentname = l[1] ;
					if(verbose)System.out.println("//opening: " + currentname) ;
				}else if(l[0].equals("End")){//End Site
					currentname = "End-"+amountunnamed ;
					amountunnamed++;
				}else if(l[0].equals("}")){
					if(verbose)System.out.println("//closing: " + currentparent.name) ;
					//when ending a section set the parent of new nodes to the grandparent
					if(currentparent.parent!=null){
						currentparent = currentparent.parent ;
					}
				}else if(l[0].equals("OFFSET")){
					//create the new joint with the offset and make it the current parent
					Joint j = new Joint(currentparent, new Vector3d( parse(l[1]), parse(l[2]),parse(l[3]) ), currentname) ;
					currentparent = j ;
					if(verbose)System.out.println("//offset: " + currentname) ;
				}else if(l[0].equals("CHANNELS")){
					int amount = Integer.parseInt(l[1]) ;
					for(int k=0;k<amount;k++){
						//save each parameter
						parametertype.add(l[k+2]) ; // what the parameter is
						parameterobject.add(currentparent.name) ; // what piece it is on
					}
				}else if(l[0].equals("MOTION")){
					line = br.readLine() ; // read line
					l = line.split(":");//split 
					frameamount = Integer.parseInt(l[1].trim()) ; // save amount of frame
					line = br.readLine() ; // read line
					l = line.split(":");//split 
					frametime = parse(l[1]) ; // save frametime
					
					
					if(verbose){
						System.out.println("Parameters:" ) ;
						for(int k=0;k<parameterobject.size();k++){
							System.out.println(parameterobject.get(k) + " - " + parametertype.get(k)) ;
						}
					}
					
					//save the baseskeleton
					baseskeletonroot = currentparent ;
					jointnames = baseskeletonroot.getJointNames() ;
					frames = new ArrayList<Frame>(frameamount) ;
					double currentframetime = 0 ;
					
					
					
					//build the animation
					for(int k=0;k<frameamount;k++){
						Joint frameroot = baseskeletonroot.duplicateSkeleton() ;
						//build a table so we can map object names to their joints in the new skeleton
						Hashtable<String, Joint> jointtable = new Hashtable<String, Joint>() ;
						frameroot.addSkeletontoNameHash(jointtable) ;
						
						line = br.readLine() ; // read line
						l = line.split("\\s+");//split on whitespace
						if(verbose) System.out.println("Parsing Frame: " + k) ;
						//apply parameters to skeleton
						for(int p=0;p<l.length ;p++){
							//figure out what the parameter is
							String type = parametertype.get(p) ;//string describing what this parameter does
							Joint target = jointtable.get(parameterobject.get(p)) ;//use name to look up joint in cloned skeleton
							double value = parse(l[p]) ;//pull value out of file
							if(verbose)System.out.println(type +" - " + target.name + " - " + value) ;
							//Note that changes will take place in the order they appear in the file!
							if(type.equals("Xposition")){
								target.baseoffset.x = value ;
							}else if(type.equals("Yposition")){
								target.baseoffset.y = value ;
							}else if(type.equals("Zposition")){
								target.baseoffset.z = value ;
							}
							
							else if(type.equals("Zrotation")){
								
								Quat4d rot = new Quat4d() ;
								rot.setAxisAngle(0, 0, 1, value*degreestoradians) ;
								target.appliedrotation.mul(rot) ;
								
								
							}else if(type.equals("Xrotation")){
								Quat4d rot = new Quat4d() ;
								rot.setAxisAngle(1, 0, 0, value*degreestoradians) ;
								target.appliedrotation.mul(rot) ;
							}else if(type.equals("Yrotation")){
								Quat4d rot = new Quat4d() ;
								rot.setAxisAngle(0, 1, 0, value*degreestoradians) ;
								target.appliedrotation.mul(rot) ;
							}
							
						}
						
						//TODO
						
						
						//save frame
						frameroot.setGlobalTransform() ;
						frames.add(new Frame(frameroot, currentframetime));
						currentframetime+= frametime ;
						animationlength = currentframetime ;
						
					}
					
				}
			}
		}catch(Exception e){
			e.printStackTrace() ;
			System.err.print(e) ;
		}
		
	}
	
	//makes the code for parsing a double slightly shorter
	public static double parse(String s){
		return Double.parseDouble(s) ;
	}
	
	//returns the skeleton of a frame at the given time in seconds
	public Joint getframe(double time){
		int f = (int)(time/frametime) ;
		if(f < 0) f = 0 ;
		if(f >= frames.size()) f = frames.size()-1 ;
		return frames.get(f).root ;
		
	}
	
	//returns an axis aligned bounding box of the entire motion
	//in the form (minx,miny,minz,maxx,maxy,maxz
	public double[] getAABB(){
		double AABB[] = new double[]{99999,99999,99999,-99999,-99999,-99999} ;
		for(int k=0;k<frames.size();k++){
			Joint root = frames.get(k).root ;
			for(int j=0;j<jointnames.size();j++){
				Vector3d p = root.getJoint(jointnames.get(j)).getWorldPoint() ;
				if(p.x < AABB[0])AABB[0] = p.x ;
				if(p.y < AABB[1])AABB[1] = p.y ;
				if(p.z < AABB[2])AABB[2] = p.z ;
				if(p.x > AABB[3])AABB[3] = p.x ;
				if(p.y > AABB[4])AABB[4] = p.y ;
				if(p.z > AABB[5])AABB[5] = p.z ;
			}
		}
		return AABB ;
	}
	
	
	private class Frame{
		public double time ;
		public Joint root ;
		
		public Frame(Joint r, double t){
			root = r ;
			time = t ;
		}
		
	}
	
}
