import os
import sys
import numpy as np
from PIL import Image
import vtk
from vtkplotter import load

SCREENSHOT_SIZE = 250
MODEL_BOUND = 25
SQUARE_SIZE = 8


def replace_zeroes(data):
  min_nonzero = np.min(data[np.nonzero(data)])
  data[data == 0] = min_nonzero
  return data


def get_fourier_transform_coefficients(img_path, square_size=SQUARE_SIZE ,low_freq=True):
    with Image.open(img_path) as img:
        img = img.convert('L')  # To gray scale
        img = np.array(img)
        fft_img = replace_zeroes(np.fft.fft2(img))
    # print(img.shape)
    # print(fft_img.shape)
    if low_freq:
        fft_img = np.fft.fftshift(fft_img)
        return 20*np.log(np.abs(fft_img[:square_size, :square_size]))
    return 20*np.log(np.abs(fft_img[:square_size, :square_size]))


def get_silhouette_to_vector(sil_loc):
    with Image.open(sil_loc) as img:
        img = img.convert('L')  # To gray scale
        arr = np.array(img)
    flat_arr = arr.ravel()

    return flat_arr


def get_scale_factor(obj, plane):
    xmin, xmax, ymin, ymax, zmin, zmax = obj.GetBounds()
    x = abs(xmax - xmin)
    y = abs(ymax - ymin)
    z = abs(zmax - zmin)
    max_len = 1

    if plane == 'x':
        max_len = max(y, z)
    elif plane == 'y':
        max_len = max(x, z)
    elif plane == 'z':
        max_len = max(y, x)

    return MODEL_BOUND/max_len


def extract_org_filename(filename):
    filename = filename.split('/')
    filename = filename[-1].split('.')

    return filename[0]


def get_coords_for_center_positioning(obj):
    xmin, xmax, ymin, ymax, zmin, zmax = obj.GetBounds()
    x = abs(xmax - xmin)
    y = abs(ymax - ymin)
    z = abs(zmax - zmin)

    return -x/2, -y/2, -z/2


def file_to_ordered_silhouette_actor(file_name, plane):
    obj_sil = load(file_name).projectOnPlane(plane).c('r').silhouette()
    obj_sil.SetScale(get_scale_factor(obj_sil, plane))
    obj_sil.SetPosition(get_coords_for_center_positioning(obj_sil))

    return obj_sil


def create_camera_according_to_plane(plane):

    x_pos = {'x': 30, 'y': 0,  'z': 0}[plane]
    y_pos = {'x': 0, 'y': 30,  'z': 0}[plane]
    z_pos = {'x': 0, 'y': 0,  'z': 30}[plane]
    camera = vtk.vtkCamera()

    camera.SetPosition(x_pos, y_pos, z_pos)

    if plane == 'z':
        camera.SetViewUp(0, 1, 0)
    else:
        camera.SetViewUp(0, 0, 1)

    camera.SetFocalPoint(0, 0, 0)
    camera.Zoom(0.60)

    return camera


def create_scene_n_save_screenshots(file_name, folder_path=os.getcwd(), present=False, w_axes=False, prefix=False):
    planes = ['x', 'y', 'z']
    colors = vtk.vtkNamedColors()
    curr_dir = os.getcwd()

    for plane in planes:

        # create a rendering window and renderer
        renderer = vtk.vtkRenderer()
        renWin = vtk.vtkRenderWindow()

        obj_sil = file_to_ordered_silhouette_actor(file_name, plane)
        renderer.SetActiveCamera(create_camera_according_to_plane(plane))
        renderer.AddActor(obj_sil)

        if w_axes:
            axes = vtk.vtkAxesActor()
            axes.AxisLabelsOff()
            axes.SetTotalLength(100, 100, 100)
            renderer.AddActor(axes)

        renWin.AddRenderer(renderer)
        renWin.SetSize(SCREENSHOT_SIZE, SCREENSHOT_SIZE)

        # mapper
        # mapper = vtk.vtkPolyDataMapper()
        # mapper.SetInputConnection(source.GetOutputPort())

        renderer.SetBackground(colors.GetColor3d("White"))
        renWin.Render()

        if prefix:
            name = extract_org_filename(file_name) + "_sil_plane_" + plane + ".png"
        else:
            name = "sil_plane_" + plane + ".png"

        # screenshot code:
        w2if = vtk.vtkWindowToImageFilter()  # Window to image filter
        w2if.SetInput(renWin)
        w2if.SetInputBufferTypeToRGB()
        w2if.ReadFrontBufferOff()
        w2if.Update()

        if curr_dir != folder_path:
            os.chdir(folder_path)

        writer = vtk.vtkPNGWriter()
        writer.SetFileName(name)
        writer.SetInputConnection(w2if.GetOutputPort())
        writer.Write()

        if curr_dir != folder_path:
            os.chdir(curr_dir)

        if present:
            # create a renderwindowinteractor
            iren = vtk.vtkRenderWindowInteractor()
            iren.SetRenderWindow(renWin)
            # enable user interface interactor
            iren.Initialize()
            iren.Start()


def main():
    # model, save_location = sys.argv[1:]
    create_scene_n_save_screenshots('/Users/hagardolev/Documents/Computer-Science/Third_year/sara_guidance/m197.off',
                                    '/Users/hagardolev/Documents/Computer-Science/Third_year/sara_guidance/3d_model_contour_extractor/screenshots')
    coef = get_fourier_transform_coefficients('/Users/hagardolev/Documents/Computer-Science/Third_year/sara_guidance/3d_model_contour_extractor/screenshots/sil_plane_x.png', 8)
    print(20*np.log(np.abs(coef)))


if __name__ == '__main__':
    main()