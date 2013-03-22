import java.util.ArrayList;
import java.util.Hashtable;

import org.lwjgl.opengl.GL11;


public class Joint {
	// the parent of this joint or null for the root
	public Joint parent ;

	// the offset of this joint from it's parent in the original skeleton
	public Vector3d baseoffset ;

	//the current rotation applied to this joint
	public Quat4d appliedrotation ;

	//the children of this joint
	public ArrayList<Joint> children ;

	//the name of this joint
	public String name ;

	public Matrix4d globaltransform ;

	//make a new joint by adding an offset to a parent jointr
	public Joint( Joint parent, Vector3d offset, String name){
		this.parent = parent ;
		if(parent!=null){
			parent.children.add(this) ;
		}
		this.baseoffset = offset ;
		appliedrotation = new Quat4d(0,0,0,1) ;
		children = new ArrayList<Joint>() ;
		this.name = name ;
	}



	//sets the parent of this node, while maintaining children lists
	public void setparent(Joint newparent){
		if(parent!=null){//remove from old parent's child list
			parent.children.remove(this) ;
		}
		parent = newparent ;

		if(parent!=null){//we might set to null to remove from skeleton
			parent.children.add(this) ;
		}
	}

	//creates a nonshallow copy of a joint
	//attached to nothing
	public Joint(Joint j){
		this.parent = null ;
		baseoffset = new Vector3d(j.baseoffset) ; ;
		appliedrotation = new Quat4d(j.appliedrotation);
		children = new ArrayList<Joint>() ;
		name = j.name ;
	}


	//returns a nonshallow copy of this joint and all of its children
	public Joint duplicateSkeleton(){
		Joint dup = new Joint(this) ;
		for(int k=0;k<children.size();k++){
			Joint child = children.get(k).duplicateSkeleton() ;
			child.setparent(dup) ;
		}
		return dup ;
	}

	//Adds this node and all of it's children to a hastable mapping names to joint objects
	public void addSkeletontoNameHash(Hashtable<String, Joint> table){
		table.put(name, this) ;
		for(int k=0;k<children.size();k++){
			children.get(k).addSkeletontoNameHash(table) ;
		}
	}

	//Returns a child joint with the given name or null if there is none
	public Joint getJoint(String jointname){
		if(name.equals(jointname)){
			return this ;
		}else{
			for(int k=0;k<children.size(); k++){
				Joint result = children.get(k).getJoint(jointname) ;
				if(result!=null){
					return result ;
				}
			}
			return null ;
		}
	}

	//returns the names of all joints in this tree
	public ArrayList<String> getJointNames(){
		ArrayList<String> names = new ArrayList<String>() ;
		addJointNames(names);
		return names ;
	}

	//add the joint names of this and all lower joints to the arraylist of names
	public void addJointNames(ArrayList<String> names){
		names.add(name);
		for(int k=0;k<children.size(); k++){
			children.get(k).addJointNames(names) ;
		}
	}

	//returns the location of the end of this joint in world space assuming the global transform has been set
	public Vector3d getWorldPoint(){
		Vector4d a = new Vector4d(baseoffset.x,baseoffset.y,baseoffset.z, 1);
		globaltransform.transform(a) ;

		return new Vector3d(a.x,a.y,a.z) ;
	}


	//Returns a child joint whose segment is closest to the given point
	public Joint getClosestJoint(Vector3d v){
		Joint closest = this ;
		double closeness = distancesquared(v) ;

		for(int k=0;k<children.size(); k++){
			Joint result = children.get(k).getClosestJoint(v) ;
			double c = result.distancesquared(v) ;
			if(c < closeness){
				closeness = c ;
				closest = result ;
			}
		}
		return closest	;

	}

	//returns the squared distance to the segment connecting this joint to its parent
	public double distancesquared(Vector3d v){
		if(parent == null){//cannot bind to root, it has no segment
			return Double.MAX_VALUE ;
		}
		Vector4d a = new Vector4d(0,0,0,1) ;
		Vector4d b = new Vector4d(baseoffset.x, baseoffset.y, baseoffset.z,1) ;
		globaltransform.transform(a) ;
		globaltransform.transform(b) ;
		Vector3d av = new Vector3d(v.x-a.x, v.y-a.y, v.z-a.z) ;
		Vector3d ab = new Vector3d(b.x-a.x, b.y-a.y, b.z-a.z) ;

		double t = Math.max(0, Math.min(1,av.dot(ab)/ab.dot(ab))) ;
		double dx = a.x + t * ab.x - v.x ;
		double dy = a.y + t * ab.y - v.y ;
		double dz = a.z + t * ab.z - v.z ;
		return dx*dx + dy*dy + dz*dz ;
	}

	//sets the Global transform of this object assuming its parent's is set
	//then recursively calls on children
	public void setGlobalTransform(){

		if(parent != null){
			Matrix4d parenttransform = new Matrix4d(parent.globaltransform);
			Vector3d parenttranslation = new Vector3d(0, 0, 0);
			parenttransform.get(parenttranslation);
			parenttransform.setTranslation(new Vector3d(0, 0, 0));
			Vector4d mytranslation = new Vector4d(parent.baseoffset);
			parenttransform.transform(mytranslation);
			Matrix4d mytransform = new Matrix4d(parent.appliedrotation, new Vector3d(0,0,0), 1) ;
			parenttransform.mul(mytransform) ;
			parenttransform.setTranslation(new Vector3d(mytranslation.x + parenttranslation.x, mytranslation.y + parenttranslation.y, mytranslation.z + parenttranslation.z));
			globaltransform = new Matrix4d(parenttransform) ;
		}else{
			//globaltransform = new Matrix4d(appliedrotation, new Vector3d(), 1) ;
			globaltransform = new Matrix4d() ;
			globaltransform.setIdentity();
		}

		for(int k=0;k<children.size();k++){
			children.get(k).setGlobalTransform() ;
		}
	}

	//sets the bone lengthso f this skeleton to match another skeleton
	public void setBoneLengths(Joint otherskeleton){
		Joint matchingbone = otherskeleton.getJoint(name) ;
		if(parent!=null && matchingbone!=null){
			baseoffset.scale(Math.sqrt(matchingbone.baseoffset.lengthSquared()/baseoffset.lengthSquared())) ;
		}
		for(int k=0;k<children.size();k++){
			children.get(k).setBoneLengths(otherskeleton) ;
		}

	}


	//draws cylinders around the skeleton bones
	public void drawCylinders(double radius, int points){
		if(parent!=null){

			//get axis along bone
			Vector3d boneaxis = new Vector3d(baseoffset.x, baseoffset.y, baseoffset.z) ;
			boneaxis.normalize();
			Vector3d up = new Vector3d(-.02f,0.01f,1) ;
			Vector3d right = new Vector3d();
			right.cross(boneaxis, up) ;
			up.cross(boneaxis,right) ;
			right.normalize();
			up.normalize() ;
			GL11.glBegin(GL11.GL_QUADS) ;
			for(int p=0; p <points; p++){
				//map a circle around each bone to be in terms of the up and right vectors
				double u1 = Math.sin(2*Math.PI*((double)p) / points) * radius ;
				double r1 = Math.cos(2*Math.PI*((double)p) / points) * radius ;
				double x1 = u1 * up.x + r1 * right.x ;
				double y1 = u1 * up.y + r1 * right.y ;
				double z1 = u1 * up.z + r1 * right.z ;
				double u2 = Math.sin(2*Math.PI*(p+1.0) / points) * radius ;
				double r2 = Math.cos(2*Math.PI*(p+1.0) / points) * radius ;
				double x2 = u2 * up.x + r2 * right.x ;
				double y2 = u2 * up.y + r2 * right.y ;
				double z2 = u2 * up.z + r2 * right.z ;
				//make the quad from neighboring points at each end of the bone
				Vector4d quad[] = new Vector4d[]{
						new Vector4d(x1,y1,z1,1),
						new Vector4d(x2,y2,z2,1),
						new Vector4d(x2+baseoffset.x,y2+baseoffset.y,z2+baseoffset.z,1),
						new Vector4d(x1+baseoffset.x,y1+baseoffset.y,z1+baseoffset.z,1)
				} ;

				//apply the global transform 
				for(int k=0;k<quad.length; k++){
					globaltransform.transform(quad[k]) ;
				}

				//calculate the normal of the quad from the transformed points
				Vector3d normal = new Vector3d() ;
				normal.cross(
						new Vector3d(quad[1].x - quad[0].x, quad[1].y - quad[0].y, quad[1].z - quad[0].z),  
						new Vector3d(quad[2].x - quad[0].x, quad[2].y - quad[0].y, quad[2].z - quad[0].z)
				);
				normal.normalize() ;

				for(int k=0;k<quad.length; k++){
					//Draw quads with normals using whatever mode openGL is currently in
					//System.out.println(quad[k].x +", " + quad[k].y +", " + quad[k].z) ;
					GL11.glVertex3d(quad[k].x,quad[k].y,quad[k].z) ;
					GL11.glNormal3d(normal.x, normal.y, normal.z) ;

				}
			}
			GL11.glEnd();


		}

		for(int k=0;k<children.size();k++){
			children.get(k).drawCylinders(radius,points) ;
		}



	}

	@Override
	public int hashCode(){
		return name.hashCode() ;
	}


}


