import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class Obj {
	public Vector3d[] vertices;
	public Vector3d[] normals;
	public Vector2d[] textureUVs;
	public Face[] faces;
	
	public Obj(File Afile){
		try{
			ArrayList<Vector3d> verticesArray = new ArrayList<Vector3d>();
			ArrayList<Vector3d> normalsArray = new ArrayList<Vector3d>();
			ArrayList<Vector2d> textureUVsArray = new ArrayList<Vector2d>();
			ArrayList<Face> facesArray = new ArrayList<Face>();
			vertices = new Vector3d[1];
			normals = new Vector3d[1];
			textureUVs = new Vector2d[1];
			faces = new Face[1];
			
			BufferedReader br = new BufferedReader(new FileReader(Afile));
			while(br.ready()){
				String line = br.readLine().trim() ; // read line
				String l[] = line.split("\\s+");//split on whitespace
				if (l[0].equals("v")) {//if this is a vertex
					verticesArray.add(new Vector3d(Double.parseDouble(l[1]), 
					Double.parseDouble(l[2]), Double.parseDouble(l[3])));
				}
				else {
					if (l[0].equals("vn")) {
						normalsArray.add(new Vector3d(Double.parseDouble(l[1]), 
						Double.parseDouble(l[2]), Double.parseDouble(l[3])));
					}
					else {
						if (l[0].equals("vt")) {
							textureUVsArray.add(new Vector2d(Double.parseDouble(l[1]), 
							Double.parseDouble(l[2])));
						}
						else {
							if (l[0].equals("f")) {
								facesArray.add(new Face(l));
							}
						}
					}
				}
			}
			vertices = verticesArray.toArray(vertices);
			normals = normalsArray.toArray(normals);
			textureUVs = textureUVsArray.toArray(textureUVs);
			faces = facesArray.toArray(faces);			
		}
		catch(Exception e){
			e.printStackTrace() ;
			System.err.print(e) ;
		}
	}
}