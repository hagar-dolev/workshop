import sys
import os
import matplotlib.pyplot as plt
from PIL import Image
from model_to_vector import create_scene_n_save_screenshots
import json


def show_search_results(imgs_paths):
    ## shows up to 10 results

    fig,ax = plt.subplots(2, 5)
    fig.set_size_inches((20,10))
    min_index = min([len(imgs_paths), 10])
    for i in range(min_index):
        with Image.open(imgs_paths[i]) as image:
            # image=Image.open(f)
            ax[i%2][i//2].imshow(image)
    # fig.show()
    # plt.axis('off')
    plt.show()


def create_models_thumbnail(models_paths, temp_path, show_all_angles=False):
    for model_p in models_paths:
        create_scene_n_save_screenshots(model_p, temp_path, prefix=True)

    # for x in os.listdir(temp_path):
    #     print(os.path.abspath(x))

    if show_all_angles:
        return [os.path.abspath(os.path.join(temp_path, x)) for x in os.listdir(temp_path)]

    return [os.path.abspath(os.path.join(temp_path, x)) for x in os.listdir(temp_path) if x.endswith("_sil_plane_x.png")]


def extract_files_paths(results_path):
    paths = []
    with open(results_path) as json_file:
        result = json.load(json_file)
        for res in result:
            paths.append(res['src'])

    return paths


def main():
    # results_path = "/Users/hagardolev/Documents/Computer-Science/Third_year/sara_guidance/resultsOne.json"
    results_path = sys.argv[1]
    temp_path = sys.argv[2]

    if len(sys.argv) == 4:
        show_all = sys.argv[3]
    else:
        show_all = False

    models_paths = extract_files_paths(results_path)
    imgs_to_show = create_models_thumbnail(models_paths, temp_path, show_all)
    print(imgs_to_show)
    show_search_results(imgs_to_show)


if __name__ == '__main__':
    main()