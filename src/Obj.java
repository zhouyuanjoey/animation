import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

public class Obj {
	public Vector3d[] vertices; // contain vertices
	public Vector3d[] normals; // normals
	public Vector2d[] textureUVs; // texture coordinates
	public int[][][] faces; // indices are [which face][which point on face] and then {vertex index, texture index, normal index}

	public Obj(File Afile){
		try{
			//make arraylists since we don't know how many we have yet
			ArrayList<Vector3d> verticesArray = new ArrayList<Vector3d>();
			ArrayList<Vector3d> normalsArray = new ArrayList<Vector3d>();
			ArrayList<Vector2d> textureUVsArray = new ArrayList<Vector2d>();
			ArrayList<int[][]> facesArray = new ArrayList<int[][]>();

			BufferedReader br = new BufferedReader(new FileReader(Afile));
			while(br.ready()){
				String line = br.readLine().trim() ; // read line
				String l[] = line.split("\\s+");//split on whitespace
				if (l[0].equals("v")) {//if this is a vertex
					verticesArray.add(new Vector3d(Double.parseDouble(l[1]),Double.parseDouble(l[2]), Double.parseDouble(l[3])));
				}else if (l[0].equals("vn")) { // if a normal
					normalsArray.add(new Vector3d(Double.parseDouble(l[1]), Double.parseDouble(l[2]), Double.parseDouble(l[3])));
				}else if (l[0].equals("vt")) {// if a texture coordinate
					textureUVsArray.add(new Vector2d(Double.parseDouble(l[1]), Double.parseDouble(l[2])));
				}else if (l[0].equals("f")) {//if a face
					int face[][] = new int[(l.length-1)][] ;
					for(int k=1;k<l.length;k++){ // for each point on the face
						String s[] = l[k].split("/");//pull the three indices for vertex, texture, and normal
						face[k-1] = new int[]{Integer.parseInt(s[0]),Integer.parseInt(s[1]), Integer.parseInt(s[2])} ;
					}
					facesArray.add(face);

				}
			}
			//move Arraylists into raw arrays
			vertices = verticesArray.toArray(new Vector3d[verticesArray.size()]);
			normals = normalsArray.toArray(new Vector3d[normalsArray.size()]);
			textureUVs = textureUVsArray.toArray(new Vector2d[textureUVsArray.size()]);
			faces = facesArray.toArray(new int[facesArray.size()][][]);			
		}
		catch(Exception e){
			e.printStackTrace() ;
			System.err.print(e) ;
		}
	}
	
	//creates a copy of an object file where every vertex has its own normal and UVs with matching index
	public Obj(Obj a){
		vertices = new Vector3d[a.vertices.length]; // contain vertices
		normals = new Vector3d[a.vertices.length]; // normals
		textureUVs = new Vector2d[a.vertices.length]; // texture coordinates
		faces = new int[a.faces.length][][]; // indices are [which face][which point on face] and then {vertex index, texture index, normal index}

		for(int k=0;k<vertices.length;k++){
			vertices[k] = new Vector3d(a.vertices[k].x, a.vertices[k].y, a.vertices[k].z) ;
		}
		
		for(int k=0;k<faces.length;k++){
			faces[k] = new int[a.faces[k].length][3];
			for(int j=0;j<faces[k].length;j++){
				faces[k][j][0] = a.faces[k][j][0];
				faces[k][j][1] = a.faces[k][j][0];//they all use the vertex index since we're rearranging the other arrays to match
				faces[k][j][2] = a.faces[k][j][0];
				int v = faces[k][j][0]-1, n = a.faces[k][j][2] -1, t = a.faces[k][j][1]-1 ;
				normals[v] = new Vector3d( a.normals[n].x, a.normals[n].y, a.normals[n].z) ;
				textureUVs[v] = new Vector2d(a.textureUVs[t].x, a.textureUVs[t].y) ;
			}
		}
	}

	//returns the axis aligned bounding box of this model
	public double[] getAABB(){
		double AABB[] = new double[]{99999,99999,99999,-99999,-99999,-99999} ;
		for(int k=0;k < vertices.length;k++){
			Vector3d p= vertices[k];
			if(p.x < AABB[0])AABB[0] = p.x ;
			if(p.y < AABB[1])AABB[1] = p.y ;
			if(p.z < AABB[2])AABB[2] = p.z ;
			if(p.x > AABB[3])AABB[3] = p.x ;
			if(p.y > AABB[4])AABB[4] = p.y ;
			if(p.z > AABB[5])AABB[5] = p.z ;
		}
		return AABB ;

	}

	public void draw(){

		for(int k=0;k<faces.length;k++){
			GL11.glBegin(GL11.GL_POLYGON) ;
			for(int j=0;j<faces[k].length;j++){
				try{
					Vector3d v = vertices[faces[k][j][0]-1] ;
					Vector2d t = textureUVs[faces[k][j][1]-1] ;
					Vector3d n = normals[faces[k][j][2]-1] ;
					GL11.glTexCoord2d(t.x, t.y) ;
					GL11.glNormal3d(n.x, n.y, n.z) ;
					GL11.glVertex3d(v.x, v.y, v.z) ;
					
				}catch(Exception e){
					System.out.println(e) ;
					e.printStackTrace() ;
				}
			}
			GL11.glEnd();
		}


	}
}