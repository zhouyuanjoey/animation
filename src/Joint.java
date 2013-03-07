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

	public void drawLines(){
		if(parent!=null){
			if(children.size() < 1){
				GL11.glColor3f(1, 1, 0) ;
			}else{
				GL11.glColor3f(1, 1, 1) ;
			}
			Vector3d[] perpendicular = new Vector3d[3]; 
			perpendicular[0] = new Vector3d(baseoffset.z, baseoffset.z, -baseoffset.x - baseoffset.y);
			perpendicular[1] = new Vector3d(-baseoffset.y - baseoffset.z, baseoffset.x, baseoffset.x);
			perpendicular[2] = new Vector3d(baseoffset.y, -baseoffset.z - baseoffset.x, baseoffset.y);
			int index = 0;
			double maxLength = perpendicular[0].lengthSquared();
			for (int i = 1; i <= 2; i++)
			if (perpendicular[i].lengthSquared() > maxLength) {
				index = i;
				maxLength =  perpendicular[i].lengthSquared();
			}
			perpendicular[index].normalize();
			Vector4d []startPoints = new Vector4d[24];
			Vector4d []endPoints = new Vector4d[24];
			Vector4d p2 = new Vector4d(baseoffset.x, baseoffset.y, baseoffset.z, 1);
			Vector4d p2n = new Vector4d();
			p2n.normalize(p2);
			Quat4d rot = new Quat4d();
			Matrix4d rotp2 = new Matrix4d();
			for (int i = 0; i < 24; i++) {
				rot.setAxisAngle(p2n.x, p2n.y, p2n.z, Math.PI / 12 * i);
				startPoints[i] = new Vector4d(3 * perpendicular[index].x, 3 * perpendicular[index].y, 3 * perpendicular[index].z, 1);
				rotp2.set(rot);
				rotp2.transform(startPoints[i]);
				endPoints[i] = new Vector4d(startPoints[i].x + p2.x, startPoints[i].y + p2.y, startPoints[i].z + p2.z, 1);
				globaltransform.transform(startPoints[i]);
				globaltransform.transform(endPoints[i]);
				GL11.glColor3f((float)i / 24, 1 - (float)i / 24, 0.0f);
				drawLine(startPoints[i], endPoints[i]) ;
			}
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


