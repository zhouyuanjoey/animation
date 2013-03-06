import java.util.ArrayList;
import java.util.Hashtable;

import org.lwjgl.opengl.GL11;


public class Joint {
	// the parent of this joint or null for the root
	Joint parent ;

	// the offset of this joint from it's parent in the original skeleton
	Vector3d baseoffset ;

	//the current rotation applied to this joint
	Quat4d appliedrotation ;

	//the children of this joint
	ArrayList<Joint> children ;

	//the name of this joint
	String name ;

	Matrix4d globaltransform ;

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


	//sets the Global transform of this object assuming its parent's is set
	//then recursively calls on children
	public void setGlobalTransform(){

		if(parent != null){
			Matrix4d parenttransform = new Matrix4d(parent.globaltransform);
			Matrix4d mytransform = new Matrix4d(parent.appliedrotation, new Vector3d(0,0,0), 1) ;

			parenttransform.mul(mytransform) ;
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

	public void drawLines(){
		if(parent!=null){
			if(children.size() < 1){
				GL11.glColor3f(1, 1, 0) ;
			}else{
				GL11.glColor3f(1, 1, 1) ;
			}
			Vector4d tp1 = new Vector4d(0, 0, 0, 1);
			Joint thisParent = parent;
			while (thisParent != null) {
				Vector4d p1 = new Vector4d(thisParent.baseoffset.x, thisParent.baseoffset.y, thisParent.baseoffset.z, 1);
				thisParent.globaltransform.transform(p1) ;
				tp1.add(p1);
				thisParent = thisParent.parent;	
			}
			tp1.w = 1.0;
			Vector4d tp2 = new Vector4d(0, 0, 0, 1);
			Joint thisObject = this;
			while (thisObject != null) {
				Vector4d p2 = new Vector4d(thisObject.baseoffset.x, thisObject.baseoffset.y, thisObject.baseoffset.z, 1);
				thisObject.globaltransform.transform(p2) ;
				tp2.add(p2);
				thisObject = thisObject.parent;
			}
			tp2.w = 1.0;

			drawLine(tp1, tp2) ;
			//System.out.println(name) ;
		}

		for(int k=0;k<children.size();k++){
			children.get(k).drawLines() ;
		}

	}
	public void drawLine(Vector4d a, Vector4d b){
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(a.x,a.y, a.z);
		GL11.glVertex3d(b.x,b.y, b.z);
		GL11.glEnd();

	}

}


